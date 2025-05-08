package vlbl.stats.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.rest.*;
import org.opensearch.search.builder.SearchSourceBuilder;
import vlbl.stats.api.ApiErrorCodeEnum;
import vlbl.stats.api.ApiResponse;
import vlbl.stats.api.ApiResponseError;
import vlbl.stats.api.CommonResponseErrorInfo;
import vlbl.stats.util.DummyLenientTypeAdapterFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.opensearch.rest.RestRequest.Method.GET;

public class StatHandler extends BaseRestHandler {
    private final static String HANDLER_NAME = "stats_action";
    private final static String ROUTE_STAT_TEMPLATE = "/_custom-stats/{index}/{function}";

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new DummyLenientTypeAdapterFactory())
            .serializeSpecialFloatingPointValues()
            .create();

    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, ROUTE_STAT_TEMPLATE)
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String index = request.param("index");
        String function = request.param("function");

        if (index == null || function == null) {
            return sendResponse(ApiResponse.error(new ApiResponseError(
                    ApiErrorCodeEnum.BAD_REQUEST,
                    "Параметры `index` и `function` являются обязательными"
            )));
        }

        Optional<StatFunctionEnum> func = StatFunctionEnum.getByName(function);

        if (func.isEmpty()) {
            return sendResponse(ApiResponse.error(new ApiResponseError(
                    ApiErrorCodeEnum.BAD_REQUEST,
                    String.format(
                            "Указана неподдерживаемая функция: '%s', доступны следующие: %s",
                            function, StatFunctionEnum.getFunctionLowerCaseNames()
                    )
            )));
        }

        Optional<ApiResponseError> verificationIndexError = verifyIndexExistence(client, index);

        return verificationIndexError
                .map(e -> sendResponse(ApiResponse.error(e)))
                .orElseGet(() -> processRequest(client, index, func.get()));
    }

    private RestChannelConsumer sendResponse(ApiResponse<?> response) {
        return channel -> {
            RestStatus status = response.isSuccess() ?
                    RestStatus.OK : RestStatus.fromCode(response.getErrorStatus().getHttpCode());

            String jsonBody = gson.toJson(response);

            channel.sendResponse(new BytesRestResponse(status, jsonBody));
        };
    }

    private Optional<ApiResponseError> verifyIndexExistence(
            NodeClient client,
            String index
    ) {
        try {
            client.admin()
                    .indices()
                    .prepareGetFieldMappings(index)
                    .get();
        } catch (IndexNotFoundException e) {
            return Optional.of(new ApiResponseError(
                    ApiErrorCodeEnum.INDEX_NOT_FOUND,
                    String.format("Индекс %s не существует", index)
            ));
        }
        return Optional.empty();
    }

    private RestChannelConsumer processRequest(NodeClient client, String index, StatFunctionEnum func) {
        ApiResponse<?> response;
        try {
            // можно и в мапу засунуть, а затем по ключу вызывать конкретную функцию, но их не так много :)
            response = switch (func) {
                case StatFunctionEnum.AVG -> avg(client, index);
                case StatFunctionEnum.MAX -> max(client, index);
                case StatFunctionEnum.VALUES -> uniqueValues(client, index);
            };
        } catch (Exception e) {
            response = ApiResponse.error(new ApiResponseError(ApiErrorCodeEnum.INTERNAL_ERROR, e.getMessage()));
        }

        return sendResponse(response);
    }

    private ApiResponse<Map<String, Double>> avg(NodeClient client, String index) {

        SearchContext ctx = defaultSearchContext(index);

        AtomicReference<Double> sum = new AtomicReference<>(0d);
        AtomicLong cnt = new AtomicLong(0);
        final List<CommonResponseErrorInfo> errorAccumulator = new LinkedList<>();

        doInSearchLoop(client, ctx, response -> toStatModelStream(response, errorAccumulator)
                .filter(stat -> nonNull(stat.batteryRunTimeRemaining()))
                .forEach(stat -> {
                    sum.getAndUpdate(s -> s + stat.batteryRunTimeRemaining());
                    cnt.incrementAndGet();
                })
        );

        Double avg = cnt.get() == 0 ? Double.NaN : sum.get() / cnt.get();
        Map<String, Double> res = Map.of(StatFunctionEnum.AVG.name().toLowerCase(), avg);

        if (errorAccumulator.isEmpty()) {
            return ApiResponse.success(res);
        }

        ApiResponse<Map<String, Double>> response = new ApiResponse<>(
                ApiErrorCodeEnum.INVALID_INDEX,
                res,
                errorAccumulator
        );
        response.setPartialResponse(true);
        return response;
    }

    private record SearchContext(
            SearchRequest request,
            TimeValue requestTimeout
    ) {
    }

    private SearchContext defaultSearchContext(String index) {
        return new SearchContext(defaultSearchRequest(index), TimeValue.timeValueMinutes(1));
    }

    private SearchRequest defaultSearchRequest(String index) {
        /*
         * Можно было бы и обрабатываемое поле включить в запрос поиска,
         * но в задании сказано, что должен происходить маппинг модели через gson,
         * поэтому вытягиваемые все обрабатываемые поля и происходит валидация индекса вначале
         * */
        String[] targetFields = StatModel.jsonFieldNames.toArray(String[]::new);
        return new SearchRequest(index)
                .source(new SearchSourceBuilder().fetchSource(targetFields, null))
                .scroll(TimeValue.timeValueMinutes(1));
    }

    private Stream<StatModel> toStatModelStream(
            SearchResponse response,
            List<CommonResponseErrorInfo> mappingErrorAccumulator
    ) {
        return Arrays.stream(response.getHits().getHits())
                .map(hit -> {
                    try {
                        return gson.fromJson(hit.getSourceAsString(), StatModel.class);
                    } catch (Exception e) {
                        /*
                        * Здесь возможно не стоит выдавать пользователю столь полную информацию (вдруг конфиденциальные данные были бы).
                        * Вместо этого можно сохранять служебную часть: hitId, docId, hit.getSourceAsString(), e.getMessage()
                        * в какую-то лог систему (тот же Elastic/OpenSearch) и выдавать пользователю сообщение + id это записи
                        * */
                        mappingErrorAccumulator.add(new CommonResponseErrorInfo(
                                ApiErrorCodeEnum.MAPPING_ERROR,
                                String.format(
                                        "Не удалось произвести маппинг для хита %s в документе %s; Данные:\n %s ; \n Содержание ошибки:\n %s",
                                        hit.getId(),
                                        hit.docId(),
                                        hit.getSourceAsString(),
                                        e.getMessage()
                                )
                        ));
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    private void doInSearchLoop(NodeClient client, SearchContext context, Consumer<SearchResponse> action) {
        SearchResponse response = null;
        String scrollId = null;
        try {
            response = client.search(context.request).actionGet(context.requestTimeout);
            scrollId = response.getScrollId();
            action.accept(response);

            while (response.getHits().getHits().length > 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(context.request.scroll());
                response = client.searchScroll(scrollRequest).actionGet(context.requestTimeout);
                action.accept(response);
            }
        } finally {
            if (nonNull(scrollId)) {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);
                client.clearScroll(clearScrollRequest);
            }
        }
    }

    private ApiResponse<Map<String, Double>> max(NodeClient client, String index) {

        SearchContext ctx = defaultSearchContext(index);

        AtomicReference<Double> max = new AtomicReference<>(Double.NaN);
        final List<CommonResponseErrorInfo> errorAccumulator = new LinkedList<>();

        doInSearchLoop(client, ctx, response -> toStatModelStream(response, errorAccumulator)
                .filter(stat -> nonNull(stat.outputVoltage()))
                .forEach(stat -> {
                    if (Double.isNaN(max.get())) {
                        max.set(stat.outputVoltage());
                    } else {
                        max.set(Double.max(stat.outputVoltage(), max.get()));
                    }
                })
        );

        Map<String, Double> res = Map.of(StatFunctionEnum.MAX.name().toLowerCase(), max.get());

        if (errorAccumulator.isEmpty()) {
            return ApiResponse.success(res);
        }

        ApiResponse<Map<String, Double>> response = new ApiResponse<>(
                ApiErrorCodeEnum.INVALID_INDEX,
                res,
                errorAccumulator
        );
        response.setPartialResponse(true);
        return response;
    }

    private ApiResponse<Map<String, Set<String>>> uniqueValues(NodeClient client, String index) {

        SearchContext ctx = defaultSearchContext(index);

        final Set<String> uniq = new HashSet<>();
        final List<CommonResponseErrorInfo> errorAccumulator = new LinkedList<>();

        doInSearchLoop(client, ctx, response -> toStatModelStream(response, errorAccumulator)
                .filter(stat -> nonNull(stat.host()))
                .forEach(stat -> uniq.add(stat.host()))
        );

        Map<String, Set<String>> res = Map.of(StatFunctionEnum.VALUES.name().toLowerCase(), uniq);

        if (errorAccumulator.isEmpty()) {
            return ApiResponse.success(res);
        }

        ApiResponse<Map<String, Set<String>>> response = new ApiResponse<>(
                ApiErrorCodeEnum.INVALID_INDEX,
                res,
                errorAccumulator
        );
        response.setPartialResponse(true);
        return response;
    }
}
package vlbl.stats.api;

import java.util.Arrays;
import java.util.Optional;

public enum ApiErrorCodeEnum {
    BAD_REQUEST(1800, "Неверный формат запроса", 400),
    INVALID_INDEX(1801, "Невалидное состояние индекса", 400),
    MISSING_MANDATORY_FIELD(1802, "Отсутствует необходимое поле", 400),
    FIELD_TYPE_MISMATCH(1803, "Несоответствие типа поля", 400),
    MAPPING_ERROR(1804, "Произошла ошибка в процессе мапинга данных", 422),
    INTERNAL_ERROR(1805, "Внутренняя ошибка сервера", 500),
    INDEX_NOT_FOUND(1806, "Индекс не найден",404),
    ;
    private final int code;

    private final String description;

    private final int httpCode;

    ApiErrorCodeEnum(int code, String description, int httpCode) {
        this.code = code;
        this.description = description;
        this.httpCode = httpCode;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCode() {
        return code;
    }

    public Integer getHttpCode() {
        return httpCode;
    }

     static Optional<ApiErrorCodeEnum> getByCode(int code){
        return Arrays.stream(ApiErrorCodeEnum.values())
                .filter(e -> e.code == code)
                .findFirst();
    }
}

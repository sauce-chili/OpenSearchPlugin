package vlbl.stats.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Фабрика для создания {@link TypeAdapter}, который обеспечивает более "мягкий" режим десериализации JSON.
 * <p>
 * Этот класс оборачивает существующий {@link TypeAdapter} (делегат) и перехватывает исключения,
 * возникающие в процессе десериализации данных. В случае ошибки (например, некорректного формата или типа данных)
 * адаптер пропускает проблемное значение с помощью {@link JsonReader#skipValue()} и возвращает {@code null},
 * вместо того чтобы прервать обработку всего JSON-документа.
 * </p>
 *
 * <p>Пример использования:
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapterFactory(new DummyLenientTypeAdapterFactory())
 *     .create();
 * String jsonArray = """
 *         [{"e":5},{"4":"4d"},{",":""}]
 * """
 * gson.fromJson(jsonArray,new TypeToken<List<Map<Integer, Integer>>>(){})
 * // result: [{null=5}, {4=4}, {null=null}]
 * }</pre>
 * </p>
 *
 * <p>Примечание: адаптер перехватывает исключения {@link JsonParseException} и {@link IllegalArgumentException},
 * которые могут возникнуть при несоответствии данных ожидаемому типу или формату.</p>
 *
 * @see TypeAdapter
 * @see TypeAdapterFactory
 */
public class DummyLenientTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                try {
                    return delegate.read(in);
                } catch (JsonParseException | IllegalArgumentException e) {
                    in.skipValue(); // пропускаем некорректное значение
                    return null;
                }
            }
        };
    }
}
package vlbl.stats.plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum StatFunctionEnum {
    AVG,
    MAX,
    VALUES,
    ;

    public static Optional<StatFunctionEnum> getByName(String name) {
        try {
            return Optional.of(StatFunctionEnum.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static List<String> getFunctionLowerCaseNames() {
        return Arrays.stream(StatFunctionEnum.values())
                .map(StatFunctionEnum::name)
                .map(String::toLowerCase)
                .toList();
    }
}

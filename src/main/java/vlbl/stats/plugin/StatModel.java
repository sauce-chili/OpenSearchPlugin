package vlbl.stats.plugin;


import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Set;

public record StatModel(
        @SerializedName(JsonFields.HOST) String host,
        @SerializedName(JsonFields.UPS_ADV_BATTERY) Double batteryRunTimeRemaining,
        @SerializedName(JsonFields.UPS_ADV_VOLTAGE) Double outputVoltage
) {
    public static final class JsonFields {
        public final static String HOST = "host";
        public final static String UPS_ADV_BATTERY = "ups_adv_battery_run_time_remaining";
        public final static String UPS_ADV_VOLTAGE = "ups_adv_output_voltage";
    }

    public static String[] getJsonFieldNames() {
        return new String[]{
                JsonFields.HOST,
                JsonFields.UPS_ADV_BATTERY,
                JsonFields.UPS_ADV_VOLTAGE
        };
    }

    public final static Map<String, Set<String>> opensearchFieldType = Map.of(
            JsonFields.HOST, Set.of("text", "keyword", "ip"),
            JsonFields.UPS_ADV_BATTERY, Set.of("integer", "long", "byte","short", "double", "float", "half_float", "scaled_float"),
            JsonFields.UPS_ADV_VOLTAGE, Set.of("integer", "long", "byte","short", "double", "float", "half_float", "scaled_float")
    );
}
package vlbl.stats.api;

public class CommonResponseErrorInfo {
    private Integer code;
    private String reason;
    private String description;

    public CommonResponseErrorInfo(ApiErrorCodeEnum apiErrorCodeEnum) {
        this(apiErrorCodeEnum.getCode(), apiErrorCodeEnum.name(), apiErrorCodeEnum.getDescription());
    }

    public CommonResponseErrorInfo(ApiErrorCodeEnum apiErrorCodeEnum, String description){
        this(apiErrorCodeEnum.getCode(), apiErrorCodeEnum.name(), description);
    }

    public CommonResponseErrorInfo(Integer code, String reason, String description) {
        this.code = code;
        this.reason = reason;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }
}

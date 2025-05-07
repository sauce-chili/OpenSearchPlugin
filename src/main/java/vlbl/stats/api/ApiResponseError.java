package vlbl.stats.api;

import java.util.ArrayList;
import java.util.List;

public class ApiResponseError {
    private Integer code;
    private String message;
    private List<CommonResponseErrorInfo> errors;

    public ApiResponseError(ApiErrorCodeEnum apiErrorCodeEnum, List<CommonResponseErrorInfo> errors){
        this.code = apiErrorCodeEnum.getCode();
        this.message = apiErrorCodeEnum.getDescription();
        this.errors = errors;
    }

    public ApiResponseError(ApiErrorCodeEnum apiErrorCodeEnum){
        this(apiErrorCodeEnum, new ArrayList<>());
    }

    public ApiResponseError(ApiErrorCodeEnum apiErrorCodeEnum, String message){
        this(apiErrorCodeEnum);
        this.message = message;
    }

    public List<CommonResponseErrorInfo> getErrors() {
        return errors;
    }

    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }
}

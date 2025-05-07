package vlbl.stats.api;

import com.google.protobuf.Api;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class ApiResponse <T> {

    private ApiErrorCodeEnum errorStatus;

    private boolean success = false;

    private boolean isPartialResponse = false;

    private T data;

    private ApiResponseError error;

    public ApiResponse(){
    }

    public ApiResponse(ApiErrorCodeEnum errorStatus, T data, List<CommonResponseErrorInfo> errorInfo) {
        if (nonNull(errorStatus)){
            this.errorStatus = errorStatus;
            this.error = new ApiResponseError(errorStatus, errorInfo);
        }
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data){
        ApiResponse<T> res = new ApiResponse<>(null, data, null);
        res.success = true;
        return res;
    }

    public static <T> ApiResponse<T> error(ApiErrorCodeEnum status, List<CommonResponseErrorInfo> errorInfo){
        return new ApiResponse<>(status, null, errorInfo);
    }

    public static <T>  ApiResponse<T> error(ApiErrorCodeEnum status){
        return new ApiResponse<>(status, null, new ArrayList<>());
    }

    public static <T> ApiResponse<T> error(ApiResponseError error){
        ApiErrorCodeEnum status = ApiErrorCodeEnum.getByCode(error.getCode()).orElseThrow();
        ApiResponse<T> response = new ApiResponse<>();
        response.setErrorStatus(status);
        response.setError(error);
        return response;
    }

    public ApiResponseError getError() {
        return error;
    }

    public void setError(ApiResponseError error) {
        this.success = false;
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ApiErrorCodeEnum getErrorStatus() {
        return errorStatus;
    }

    public void setErrorStatus(ApiErrorCodeEnum errorStatus) {
        this.errorStatus = errorStatus;
        this.success = false;
    }

    public void setPartialResponse(boolean partialResponse) {
        success = false;
        isPartialResponse = partialResponse;
    }

    public boolean isPartialResponse() {
        return isPartialResponse;
    }
}

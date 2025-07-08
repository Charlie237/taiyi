package io.github.charlie237.taiyi.common;

import lombok.Data;

/**
 * 统一API响应包装类
 */
@Data
public class ApiResponse<T> {
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public ApiResponse(int code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功");
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(500, "操作失败");
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }
    
    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message);
    }

    /**
     * 设置数据并返回当前对象（用于链式调用）
     */
    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
    
    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "未授权访问");
    }
    
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message);
    }
    
    /**
     * 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "禁止访问");
    }
    
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message);
    }
    
    /**
     * 资源不存在响应
     */
    public static <T> ApiResponse<T> notFound() {
        return new ApiResponse<>(404, "资源不存在");
    }
    
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message);
    }
}

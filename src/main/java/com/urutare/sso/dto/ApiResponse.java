package com.urutare.sso.dto;

public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;

    public ApiResponse() {
    }
    
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(String msg, T data) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true; r.message = msg; r.data = data; return r;
  }
  public static <T> ApiResponse<T> fail(String msg) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = false; r.message = msg; return r;
  }
  public boolean isSuccess() { return success; }
  public String getMessage() { return message; }
  public T getData() { return data; }
}

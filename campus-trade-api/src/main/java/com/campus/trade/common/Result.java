package com.campus.trade.common;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private int code;
    private String errorCode;
    private String msg;
    private String traceId;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setErrorCode("SUCCESS");
        r.setMsg("操作成功");
        r.setTraceId(TraceContext.getTraceId());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String msg) {
        return error(500, msg, "INTERNAL_ERROR");
    }

    public static <T> Result<T> error(int code, String msg) {
        return error(code, msg, "BUSINESS_ERROR");
    }

    public static <T> Result<T> error(int code, String msg, String errorCode) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setErrorCode(errorCode);
        r.setMsg(msg);
        r.setTraceId(TraceContext.getTraceId());
        return r;
    }
}

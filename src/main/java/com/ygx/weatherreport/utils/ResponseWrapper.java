package com.ygx.weatherreport.utils;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应格式
 */
@Data
public class ResponseWrapper<T> implements Serializable {
    private Integer code;   //状态码：0为成功，非0失败
    private String message; //提示信息
    private T data;         //返回的数据
    private Long timestamp;

    //私有构造方法
    private ResponseWrapper(Integer code,String messgae,T data){
        this.code = code;
        this.data = data;
        this.message = messgae;
        this.timestamp = System.currentTimeMillis();
    }

    //成功响应，不带数据
    public static <T> ResponseWrapper<T> success(){
        return new ResponseWrapper<>(0,"success",null);
    }

    //成功响应，带数据
    public static <T> ResponseWrapper<T> success(T data){
        return new ResponseWrapper<>(0,"success",data);
    }

    //成功响应，带消息和数据
    public static <T> ResponseWrapper<T> success(String message,T data){
        return new ResponseWrapper<>(0,message,data);
    }

    //失败响应
    public static <T> ResponseWrapper<T> error(Integer code,String message){
        return new ResponseWrapper<>(code,message,null);
    }

    //失败响应，默认code为-1
    public static <T> ResponseWrapper<T> error(String message){
        return new ResponseWrapper<>(-1,message,null);
    }

    public static <T> ResponseWrapper<T> error(Integer code, String message, T data) {
        return new ResponseWrapper<>(code, message, data);
    }
}

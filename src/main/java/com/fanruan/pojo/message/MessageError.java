package com.fanruan.pojo.message;

import lombok.Data;

@Data
public class MessageError {
    private int errCode;
    private String msg;

    //通用异常
    public static MessageError SUCCESS = new MessageError(0, "success");
    public static MessageError SERVER_ERROR = new MessageError(500100, "服务端异常");
    //注意  %s ，格式化字符串
    public static MessageError SERVER_BIND_ERROR = new MessageError(500101, "服务端绑定异常:%s");
    //信息异常 5002xx
    public static MessageError ClientID_ERROR = new MessageError(500201, "客户端握手信息错误，ID不正确");


    MessageError(int errCode, String msg){
        this.errCode = errCode;
        this.msg = msg;
    }
}

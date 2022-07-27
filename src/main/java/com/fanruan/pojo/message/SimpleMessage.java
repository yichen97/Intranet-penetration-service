package com.fanruan.pojo.message;

import lombok.Data;

@Data
public class SimpleMessage {
    private String msg;

    public SimpleMessage(String msg){
        this.msg = msg;
    }
}

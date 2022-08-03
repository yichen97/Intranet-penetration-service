package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RpcResponse {

    private String ID;

    private Object result;

    private Boolean status;
}
package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RpcResponse {

    private Object result;

    private Boolean status;
}
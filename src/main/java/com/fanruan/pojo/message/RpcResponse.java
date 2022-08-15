package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;


/**
 * @author Yichen Dai
 */
@Data
@Accessors(chain = true)
public class RpcResponse {

    private String ID;

    private Object result;

    private boolean binding;

    private Boolean status;
}
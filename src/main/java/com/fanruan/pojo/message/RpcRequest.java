package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RpcRequest<T> {
    private boolean reply;
    private Class<T> ServiceClass;
    private String MethodName;
    private Object[] args;
    private Class[] argTypes;
}

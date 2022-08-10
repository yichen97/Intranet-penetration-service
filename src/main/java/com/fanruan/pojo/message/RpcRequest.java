package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RpcRequest {
    // Marks whether the method delivered need loopback data
    private boolean reply;
    // Marks whether the method will create an instance requeired to be cached.
    // In the project, they are Drive( MyDriver ), Connection( MyConnection ), Statement( MyStatement ),
    // PreparedStatement( MyPreparedStatement ), ResultSet( MyResult ).
    private boolean binding;
    private String ID;
    private String IDtoInvoke;
    private Class ServiceClass;
    private String MethodName;
    private Object[] args;
    private Class[] argTypes;
}

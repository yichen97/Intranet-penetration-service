package com.fanruan.pojo.message;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Yichen Dai
 */
@Data
@Accessors(chain = true)
public class RpcRequest {
    /**
     *  Marks whether the method delivered need loopback data
     */
    private boolean reply;

    /**
     *  Marks whether the method will create an instance requeired to be cached.
     *  In the project, they are Drive( MyDriver ), Connection( MyConnection ), Statement( MyStatement ),
     *  PreparedStatement( MyPreparedStatement ), ResultSet( MyResult ).
     */
    private boolean binding;
    private String ID;
    private String IDToInvoke;
    private Class serviceClass;
    private String methodName;
    private Object[] args;
    private Class[] argTypes;
}

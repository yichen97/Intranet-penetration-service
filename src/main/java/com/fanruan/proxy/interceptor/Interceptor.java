package com.fanruan.proxy.interceptor;


import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.cache.LockAndCondition;
import com.fanruan.pojo.message.RpcRequest;
import com.fanruan.utils.Commons;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yichen Dai
 * cglib enhenced method to relize RPC
 * For example, when service execute query operation,
 * the RPC request will send to notify agent to do the same things,
 * like create connection and statement
 */
public class Interceptor implements MethodInterceptor {
    protected static final Logger logger = LogManager.getLogger();

    private Class<?> clazz;
    private SocketIOClient client;
    private Properties info;

    public Interceptor(Class<?> clazz, Properties info){
        this.clazz = clazz;
        this.info = info;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        if(InterceptorUtils.isInExcludedList(method.getName())){
            return methodProxy.invokeSuper(o, objects);
        }
        // Parameters injection of class MyDriver's construction method will be delayed util the first "connect" method was intercepted
        // Because Driver Instance is registered on the DriverManager in the static code block,
        // in which, the parameters used to fetch socket in cache is hard to pass in.
        if(info == null){
            info = (Properties) objects[1];
        }
        String agentId = info.getProperty("agentID");
        String dbName = info.getProperty("agentDBName");
        if(client == null){
            client = ClientCache.getClient(agentId, dbName);
        }

        logger.debug("start invoke " + method.getName());

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setReply(false)
                .setBinding(false)
                .setID(Commons.getID())
                .setServiceClass(clazz)
                .setMethodName(method.getName())
                .setArgs(objects)
                .setArgTypes(getArgTypes(objects));

        // Set whether the rpcResponses of this rpcRequest need to carry return value
        if(o instanceof com.fanruan.jdbc.resultset.MyResultSet){
            boolean flag = InterceptorUtils.isInReplyList(method.getName());
            if(flag) {
                rpcRequest.setReply(true);
            }
        }

        // Some instance need to be bound one-to-one, to make sure the operator happen in service
        // will be deliver to this specific corresponding instance.
        if(InterceptorUtils.isInBindList(o)){
            rpcRequest.setBinding(true);
        }

        // IDtoInvoke is an unique ID to identify an one-to-one binding relation.
        // It comes from rpcRequest in which the instance in the agent is created.
        String idToInvoke = InterceptorUtils.getInvokeHelper(o, "getID",  String.class);
        if(idToInvoke != null){
            rpcRequest.setIDToInvoke(idToInvoke);
        }

        FutureTask<Object> futureTask = new FutureTask<>(
                () -> {
                    Object res = null;
                    ClientWrapper wrapper = ClientCache.getClientWrapper(agentId, dbName);
                    LockAndCondition lac = wrapper.getLockAndCondition(rpcRequest.getID());
                    ReentrantLock lock = lac.getLock();
                    Condition condition = lac.getCondition();
                    lock.lock();
                    try{
                        byte[] bytes = ServerStater.SERIALIZER.serialize(rpcRequest);
                        client.sendEvent("RPCRequest", bytes);
                        condition.await();
                        // get res from RPC response data
                        res = lac.getResult();
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        lock.unlock();
                    }
                    return res;
                }
        );
        ServerStater.threadPool.submit(futureTask);
        Object res = futureTask.get();

        // res is not null, it indicates  the response carries data.
        // if the type of res is primitive type, An error will occur when using cast(), just return them directly.
        // And the data carried by response will never be the instance need to be bound.
        if(res != null){
            if(InterceptorUtils.isWraps(res)){
                return res;
            }
            return res;
        }


        Object returnObj = methodProxy.invokeSuper(o, objects);

        // If the return instance is corresponding with another instance in agent, set the binding ID.
        if (InterceptorUtils.isInBindList(returnObj)){
            InterceptorUtils.setInvokeHelper(returnObj, "setID", rpcRequest.getID());
        }

        return returnObj;
    }



    public Class<?>[] getArgTypes(Object[] objects){
        int n = objects.length;
        Class<?>[] argTypes = new Class[n];
        for(int i=0; i<n; i++){
            argTypes[i] = objects[i].getClass();
        }
        return argTypes;
    }
}
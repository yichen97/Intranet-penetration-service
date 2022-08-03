package com.fanruan.proxy.interceptor;


import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.cache.LockAndCondition;
import com.fanruan.pojo.message.RpcRequest;
import com.fanruan.pojo.message.RpcResponse;
import com.fanruan.serializer.Serializer;
import com.fanruan.utils.Commons;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * cglib 的代理增强方法
 * 该拦截器将所需信息序列化发送到agent，仅接受返回的状态码，不改变被增强方法的返回值
 * 起到通知 agent 调用相同方法的作用
 * 如在 service 查询器，也在 agent预先生成 connection 和 statement
 */
public class Interceptor implements MethodInterceptor {
    private Class clazz;
    private Serializer serializer;
    private SocketIOClient client;
    private Properties info;
    private byte[] res;

    public Interceptor(Class<?> clazz, Properties info){
        this.clazz = clazz;
        this.info = info;
        serializer = ServerStater.serializer;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        if(InterceptorUtils.isInExcludedList(method.getName())){
            return methodProxy.invokeSuper(o, objects);
        }
        // class MyDriver's arg injection will be delayed util the method was intercepted
        if(info == null){
            info = (Properties) objects[1];
        }

        String agentID = info.getProperty("agentID");
        String dbName = info.getProperty("dbName");

        if(client == null){
            client = ServerStater.cache.getClient(agentID, dbName);
        }


        System.out.println("start invoke " + method.getName());


        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setReply(false)
                .setID(Commons.getID())
                .setServiceClass(clazz)
                .setMethodName(method.getName())
                .setArgs(objects)
                .setArgTypes(getArgTypes(objects));

        sendAndWait(rpcRequest, agentID, dbName);


//        RpcResponse resp = (RpcResponse) serializer.deserialize(res, method.getReturnType());
//        Object result = resp.getResult();

        return methodProxy.invokeSuper(o, objects);
    }

    public String sendAndWait(RpcRequest rpcRequest, String agentID, String dbName) throws ExecutionException, InterruptedException {
        FutureTask<String> futureTask = new FutureTask<String>(
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        String res = "";
                        ClientWrapper wrapper = ClientCache.getClientWrapper(agentID, dbName);
                        LockAndCondition lac = wrapper.getLockAndCondition(rpcRequest.getID());
                        ReentrantLock lock = lac.getLock();
                        Condition condition = lac.getCondition();
                        try{
                            byte[] bytes = ServerStater.serializer.serialize(rpcRequest);
                            lock.lock();
                            client.sendEvent("RPCRequest", bytes);
                            condition.await();
                            // get res from RPC response data
                            res = null;
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            lock.unlock();
                        }
                        return res;
                    }
                }
        );
        futureTask.run();
        String res = futureTask.get();
        return res;
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
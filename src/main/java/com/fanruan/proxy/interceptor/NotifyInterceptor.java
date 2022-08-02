package com.fanruan.proxy.interceptor;


import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.pojo.message.RpcRequest;
import com.fanruan.serializer.KryoSerializer;
import com.fanruan.utils.Commons;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * cglib 的代理增强方法
 * 该拦截器将所需信息序列化发送到agent，仅接受返回的状态码，不改变被增强方法的返回值
 * 起到通知 agent 调用相同方法的作用
 * 如在 service 查询器，也在 agent预先生成 connection 和 statement
 */
public class NotifyInterceptor implements MethodInterceptor {
    private Class clazz;
    private SocketIOClient client;



    public NotifyInterceptor(Class<?> clazz, SocketIOClient client){
        this.clazz = clazz;
        this.client = client;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        if(InterceptorUtils.isInExcludedList(method.getName())){
            return methodProxy.invokeSuper(o, objects);
        }
        if(client == null){
            Properties info = (Properties) objects[1];
            client = ServerStater.cache.getClient(info.getProperty("agentID"), "/mysql");
        }

        System.out.println("start invoke " + method.getName());
        String agentID = Commons.getAgentID(client);



        KryoSerializer serializer = new KryoSerializer();
        final RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setReply(false)
                .setServiceClass(clazz)
                .setMethodName(method.getName())
                .setArgs(objects)
                .setArgTypes(getArgTypes(objects));

        FutureTask<String> futureTask = new FutureTask<String>(
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        String res = "";
                        ClientWrapper wrapper = ClientCache.getClientWrapperByID(agentID);
                        ReentrantLock lock = wrapper.getLock();
                        Condition condition = wrapper.getCondition();
                        try{
                            byte[] bytes = serializer.serialize(rpcRequest);
                            lock.lock();
                            client.sendEvent("RPCRequest", bytes);
                            condition.await();
                            res = "从缓存中获取的到的消息";
                            System.out.println(res);
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
//        RpcResponse resp = (RpcResponse) serializer.deserialize(res.getBytes(), method.getReturnType());
//        Object result = resp.getResult();
        return methodProxy.invokeSuper(o, objects);
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
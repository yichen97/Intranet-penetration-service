package com.fanruan.proxy.interceptor;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.pojo.message.RpcRequest;
import com.fanruan.serializer.KryoSerializer;
import com.fanruan.utils.Commons;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * cglib 的代理增强方法
 * 该拦截器将所需信息序列化发送到agent，并将反序列化得到的对象作为自身方法的返回值
 * 例如 resultSet 类的所有结果都要通过 agent 上的实现类获得
 */
public class FetchInterceptor implements MethodInterceptor {
    private Class clazz;
    private SocketIOClient client;


    public FetchInterceptor(Class<?> clazz, SocketIOClient client){
        this.clazz = clazz;
        this.client = client;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        if(InterceptorUtils.isInExcludedList(method.getName())){
            return methodProxy.invokeSuper(o, objects);
        }
        System.out.println("start invoke " + method.getName());

        String agentID = Commons.getAgentID(client);

        KryoSerializer serializer = new KryoSerializer();
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceClass(clazz)
                .setReply(true)
                .setMethodName(method.getName())
                .setArgs(objects);

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
        return null;

        }
}

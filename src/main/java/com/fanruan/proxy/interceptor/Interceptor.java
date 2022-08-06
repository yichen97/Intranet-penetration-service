package com.fanruan.proxy.interceptor;


import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.cache.LockAndCondition;
import com.fanruan.myJDBC.resultSet.MyResultSet;
import com.fanruan.pojo.message.RpcRequest;
import com.fanruan.pojo.message.RpcResponse;
import com.fanruan.serializer.Serializer;
import com.fanruan.utils.Commons;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * cglib 的代理增强方法
 * 起到通知 agent 调用相同方法的作用
 * 如在 service 进行查询时，也在 agent预先生成 connection 和 statement
 */
public class Interceptor implements MethodInterceptor {
    protected static final Logger logger = LogManager.getLogger();

    private Class clazz;
    private Serializer serializer;
    private SocketIOClient client;
    private Properties info;

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
        String dbName = info.getProperty("agentDBName");

        if(client == null){
            client = ServerStater.cache.getClient(agentID, dbName);
        }


        logger.debug("start invoke " + method.getName());


        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setReply(false)
                .setID(Commons.getID())
                .setServiceClass(clazz)
                .setMethodName(method.getName())
                .setArgs(objects)
                .setArgTypes(getArgTypes(objects));

        // 对于MyResult类的代理，需要多传一个sql参数作为agent段对应实例的表示
        // 否则找不到应该invoke的实例
        if(o instanceof com.fanruan.myJDBC.resultSet.MyResultSet){
            int n = objects.length;
            Object[] objectWithSuffix = new Object[n+1];

            for(int i=0; i<n; i++){
                objectWithSuffix[i] = objects[i];
            }
            String sql = ((MyResultSet) o).getSql();
            objectWithSuffix[n] = sql;

            rpcRequest.setArgs(objectWithSuffix);

            boolean flag = InterceptorUtils.isInReplyList(method.getName());
            if(flag) rpcRequest.setReply(true);
        }

        Object res = sendAndWait(rpcRequest, agentID, dbName);
        // res 不为空，说明该响应是回送报文的响应，应当返回响应中的数据
        // 如果是初始类型的包装类，不能应用类型转换，可以自动拆装箱
        if(res != null){
            if(InterceptorUtils.isWraps(res)){
                return res;
            }
            return method.getReturnType().cast(res);
        }
        return methodProxy.invokeSuper(o, objects);
    }

    public Object sendAndWait(RpcRequest rpcRequest, String agentID, String dbName) throws ExecutionException, InterruptedException {
        FutureTask<Object> futureTask = new FutureTask<Object>(
                new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Object res = null;
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
                            res = lac.getResult();
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
        Object res = futureTask.get();
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
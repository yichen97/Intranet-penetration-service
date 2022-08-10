# README

## 项目功能

​	为达到`Service` 从公网访问客户端所在内网中数据源的效果，通过运行在客户机上的代理程序代理`Service`的所有`JDBC`请求，并将查询结果返回给`Service`。实现目标，`Service`除更改使用的`JDBC`驱动外，对代理存在无感知，支持主流的包含`JDBC`支持的数据库。

`Agent`地址: [`Agent`](https://github.com/yichen97/Intranet-penetration-agent)

## 项目依赖

`Netty-socketio`与`Socket.io-client-Java`的对应关系是：

| [`netty-socketio`](https://github.com/mrniko/netty-socketio) | [`Java client`](https://github.com/socketio/socket.io-client-java) |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1.7.19                                                       | 1.0.x                                                        |
| 暂无                                                         | [Document](https://socketio.github.io/socket.io-client-java/installation.html) |

以下用`Service`指代`Socket`连接中的`socket`服务器，它也是需求查询用户内网数据源的公网服务器。

用`Agent`指代`Socket`连接中的客户端，也是运行在用户`PC`上承担远程调用`JDBC`方法的代理服务。

具体结构见下文项目结构图。

## QUICKSTART

1. 分别下载`Agent`和`Serviec`

2. 修改数据库配置和对应的SQL语句
3. 先运行`Service`中的`Test的`主函数

4. 运行`Agent`中的`Test`的主函数

即可在`Service`上观察到查询结果

```
目前只测试了mysql 数据库，但内置支持 mysql、 postgresql、 oracle、 sqlserver、 db2, 在 Agent 上注册驱动即可使用
```

## 实现方案

1.  `Service` 启动`socket`服务与 `Agent`建立连接后，可以开始使用代理进行查询。

2. `Service`端通过自实现的`JDBC`驱动，进行`JDBC`操作。驱动中使用基于`CGlib`的动态代理，对`Service`端的所有`JDBC`相关驱动类进行增强，所有方法信息会被序列化传递到`Agent`执行，并有选择地将结果回送到`Service`

## 结构与流程
<img src="/pic/project structure.jpg" alt="project structure" title="project structure">

如上图，对于`Service` 端来讲，`Agent`对其的代理是无感知的。在`Service`来看，只是调用了一个自定义的`JDBC`驱动进行查询。

这得益于驱动内部方法地重写，自定义地实现类在`Agent`和`Service`中有相同的名字，但内部实现却不相同，这使得整个RPC的流程十分灵活。

## 动态代理

动态代理是该项目中的核心，如在 `Driver`类的 `connect`方法中：返回的`Connection`就被替换为了动态代理增强过的`MyConnection`，实现对`Service`中调用的`JDBC`方法的完全代理。代理类会依靠`info`从缓存中找到命名空间（本项目中以`/dataSoure Name`来区别命名空间）对应的`socket`,将方法调用信息以`RPCReqquest`的方式序列化后发送出去。

```java
 	// In Service Source Code
	@Override
    public Connection connect(String url, Properties info) throws SQLException {
        String agentID = info.getProperty("agentID");
        String dbName = info.getProperty("agentDBName");
        if(dbName == null){
            dbName = url.split(":")[1];
            info.setProperty("agentDBName", dbName);
        }
        MyConnection myConn = (MyConnection) ProxyFactory.getProxy(MyConnection.class, info);
        myConn.setInfo(info);
        return myConn;
    }
```

RPC实体类包含如下信息：

```java
@Data
@Accessors(chain = true)
public class RpcRequest {
    // Marks whether the method delivered need loopback data
    private boolean reply;
    // Marks whether the method will create an instance requeired to be cached.
    private boolean binding;
    private String ID;
    private String IDtoInvoke;
    private Class ServiceClass;
    private String MethodName;
    private Object[] args;
    private Class[] argTypes;
}
```

在`Agent`收到`Request`的时候，会按照报文要求对方法进行调用，某些创建的实例会被缓存，以便之后调用。在本项目中，这些实例的类是：

```
Drive( MyDriver ), Connection( MyConnection ), Statement( MyStatement ), PreparedStatement( MyPreparedStatement ), ResultSet( MyResult )
```

```java
public Object invokeAsRequest(RpcRequest rpcRequest, BeanCache beanCache) {
...
        // The ID of the rpcRequest could be save as the ID of an instance
        // Because one instance can only been create just once for an unique rpcRequest
        String IDtoCache = rpcRequest.getID();
        String IDtoInvoke = rpcRequest.getIDtoInvoke();
...
```

## RPC调用

在一次RPC调用流程中，`FutureTask` 异步获取返回结果，以“生产者-消费者”模型实现一次调用的同步管理。

`ClientWrapper` 持有着各个命名空间上的`socket`。在这些`socket`上的通信，每次调用，会在`wrapper`中注册一个工具类:`LockAndCondition`，发出消息后，等待`socket`上出现对应的响应报文唤醒`FutureTask` 线程。通过锁机制，保证逻辑的正确性。

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientWrapper {
        private SocketIOClient client;
        private static Map<String, LockAndCondition> lockMap = new ConcurrentHashMap<>();


        public SocketIOClient getClient(){
                if(client == null) throw new RuntimeException("no such client");
                return client;
        }

        public LockAndCondition getLockAndCondition(String messageID){
                LockAndCondition lac = lockMap.get(messageID);
                if(lac == null){
                        ReentrantLock lock = new ReentrantLock();
                        Condition condition = lock.newCondition();
                        lac = new LockAndCondition(lock, condition);
                        lockMap.put(messageID, lac);
                }
                return lac;
        }
    
        public void removeLockAndCondition(String messageID){
                lockMap.remove(messageID);
        }
}
```



```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockAndCondition{
    private ReentrantLock lock;
    private Condition condition;
    private Object result;
    private String BindingID;

    LockAndCondition(ReentrantLock lock, Condition condition){
        this.lock = lock;
        this.condition = condition;
    }
}
```



```java
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
        ServerStater.threadPool.submit(futureTask);
        Object res = futureTask.get();
```

`socket`收到响应时解锁对应的线程。

```java
		// rpcResponse
		   nameSpace.addEventListener("RPCResponse", byte[].class, ((client, data, ackRequest) -> {
            RpcResponse rpcResponse = serializer.deserialize(data, RpcResponse.class);
            logger.debug("RPCResponse: " + (rpcResponse.getStatus() ? "success" : "fail"));

            String agentID = Commons.getAgentID(client);
            String dbName = Commons.getDBName(client);
            ClientWrapper wrapper = ClientCache.getClientWrapper(agentID, dbName);
            LockAndCondition lac = wrapper.getLockAndCondition(rpcResponse.getID());
            ReentrantLock lock = lac.getLock();
            Condition condition = lac.getCondition();
            // When a response is received, it notifies that the futuretask thread blocking on the lockandcondition 
            // If the response contains data, take it out. 
            try {
                lock.lock();
                Object resultData = rpcResponse.getResult();
                if(!rpcResponse.getStatus()){
                    logger.error(resultData);
                    resultData = null;
                }
                if(resultData != null) lac.setResult(resultData);
                condition.signal();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                lock.unlock();
            }
            wrapper.removeLockAndCondition(rpcResponse.getID());
            logger.debug("received response message, signaled condition");
        }));
```

`Service`是使用`netty`实现的高效同步非阻塞`IO`，上文的同步机制可以很大程度上利用`socket`的并发效果。

## 绑定实例

确定`Agent`上缓存实例与`Service`端实例的一一对应关系是很必要，不然程序在反射调用方法时会产生问题。

例如，对于`createStatement()`方法必须由上一步生成的`Connection`类进行调用。为了达到这一点，这些`Service`端实例必须和`Agent`端具有相同的ID。

考虑到在进行`RPC`调用回调的时候，利用时间和随机数生成了一个唯一`ID`。

```java
	public static String getID(){
        return getTimeInMillis() + getRandom();
    }

    public static String getTimeInMillis() {
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        return timeInMillis+"";
    }

    public static String getRandom() {
        Random random = new Random();
        int nextInt = random.nextInt(9000000);
        nextInt=nextInt+1000000;
        String str=nextInt+"";
        return str;
    }
```

而`Agent`端的缓存实例是由某次调用产生的，所以只需将该次调用的`RPC`报文`ID`标记在实例上，并在收到`RPC`响应时为需要绑定的类型打上同样的标记即可。这样`Agent`方面，由于存储的实例都有了唯一的`ID`作为键，大大简化了缓存系统的复杂性。

标记实现：

```java
@Override
public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {		...
		
     Object returnObj = methodProxy.invokeSuper(o, objects);

     // If the return instance is corresponding with another instance in agent, set the binding ID.
     if (InterceptorUtils.isInBindList(returnObj)){
     	InterceptorUtils.setInvokeHelper(returnObj, "setID", rpcRequest.getID());
     }
```

## 项目参考

[nuzzle: A Simple RPC Project](https://github.com/sakiila/nuzzle)

[CSV JDBC Driver](https://github.com/peterborkuti/csv-jdbc-driver)


# README

## 项目功能

​	为达到`Service` 从公网访问客户端所在内网中数据源的效果，通过运行在客户机上的代理程序代理`Service`的所有`JDBC`请求，并将查询结果返回给`Service`。

`Agent`地址: [`Agent`](https://github.com/yichen97/Intranet-penetration-agent)

## 实现方案

1.  `Service` 启动`socket`服务与 `Agent`建立连接后，可以开始使用代理进行查询。

2. `Service`端通过自实现的`JDBC`驱动，进行`JDBC`操作。驱动中使用基于`CGlib`的动态代理，对`Service`端的所有`JDBC`相关驱动类进行增强，所有方法信息会被序列化传递到`Agent`执行，并有选择地将结果回送到`Service`

## 结构与流程
<img src="/pic/project structure.jpg" alt="project structure" title="project structure">

如上图，对于`Service` 端来讲，`Agent`对其的代理是无感知的。在`Service`来看，只是调用了一个自定义的`JDBC`驱动进行查询。

这得益于驱动内部方法地重写，自定义地实现类在`Agent`和`Service`中有相同的名字，但内部实现却不相同，这使得整个RPC的流程十分灵活。

如在 `Driver`类的 `connect`方法中：

返回的`Connection`就被替换为了动态代理增强过的`MyConnection`，实现对`Service`中调用方法的完全代理。

```java
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
        futureTask.run();
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
            // response 到达时，通知正阻塞在LockAndCondition类上的FutureTask线程
            // 如果response 报文中包含数据，将数据取出
            try {
                lock.lock();
                Object resultData = rpcResponse.getResult();
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

本项目的依赖版本：

`Netty-socketio`与`Socket.io-client-Java`的对应关系是：

| `netty-socketio` | `Java client` |
| ---------------- | ------------- |
| 1.7.19           | 1.0.x         |

其中`Service`是使用`netty`实现的高效同步非阻塞`IO`，上文的同步机制可以很大程度上利用`socket`的并发效果。
package com.fanruan;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientWrapper;
import com.fanruan.cache.LockAndCondition;
import com.fanruan.pojo.message.RpcResponse;
import com.fanruan.serializer.KryoSerializer;
import com.fanruan.serializer.Serializer;
import com.fanruan.utils.Commons;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yichen Dai
 */
public class ServerStater{
    protected static final Logger logger = LogManager.getLogger();

    public static final Serializer SERIALIZER = new KryoSerializer();

    public static ExecutorService threadPool;

    public static SocketIOServer server;

    public static ClientCache cache;

    public static int workCount;



    public ServerStater(String[] dbs){
        try{
            loadConfig();
            for(String dbName : dbs){
                SocketIONamespace namespace = server.addNamespace("/" + dbName);
                addEvent(namespace);
            }
            server.start();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void addEvent(SocketIONamespace nameSpace){
        logger.debug("配置事件监听");
        nameSpace.addConnectListener(client -> {
            logger.info(nameSpace.getName() + "- socket connected!");
            String agentID = Commons.getAgentID(client);

            if(agentID == null){
                // 如果连接信息错误，发送异常信息，关闭socket
                logger.info("连接信息错误：agentID, 连接关闭");
                client.disconnect();
            }

            String dbName = Commons.getDBName(client);
            // 缓存连接
            ClientCache.saveClient(agentID, dbName, client);

        });

        // rpcResponse
        nameSpace.addEventListener("RPCResponse", byte[].class, ((client, data, ackRequest) -> {
            RpcResponse rpcResponse = SERIALIZER.deserialize(data, RpcResponse.class);
            logger.debug("RPCResponse: " + (rpcResponse.getStatus() ? "success" : "fail"));

            String agentID = Commons.getAgentID(client);
            String dbName = Commons.getDBName(client);
            ClientWrapper wrapper = ClientCache.getClientWrapper(agentID, dbName);
            LockAndCondition lac = wrapper.getLockAndCondition(rpcResponse.getID());

            ReentrantLock lock = lac.getLock();
            Condition condition = lac.getCondition();
            // When a response is received, it notifies that the FutureTask thread blocking on the LockAndCondition
            // If the response contains data, take it out.
            lock.lock();
            try {
                Object resultData = rpcResponse.getResult();
                if(!rpcResponse.getStatus()){
                    logger.error(resultData);
                    resultData = null;
                }
                if(resultData != null) {
                    lac.setResult(resultData);
                }
                condition.signal();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                lock.unlock();
            }
            wrapper.removeLockAndCondition(rpcResponse.getID());
            logger.debug("received response message, signaled condition");
        }));

        // 处理错误事件
        nameSpace.addEventListener("ErrorMessage", String.class,
                ((client, data, ackRequest) -> logger.info("Error: " + data)
        ));
    }

    private void loadConfig() throws IOException {
        logger.debug("加载配置");
        SocketConfig socketConfig = new SocketConfig();

        com.corundumstudio.socketio.Configuration config =
                new com.corundumstudio.socketio.Configuration();

        InputStream in = this.getClass().getResourceAsStream("/socketIO.properties");
        Properties props = new Properties();
        InputStreamReader inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        props.load(inputStreamReader);

        int bossCount = Integer.parseInt(props.getProperty("bossCount"));
        String host = props.getProperty("host");
        int port = Integer.parseInt(props.getProperty("port"));
        workCount = Integer.parseInt(props.getProperty("workCount"));
        boolean allowCustomRequests = Boolean.parseBoolean(props.getProperty("allowCustomRequests"));
        int upgradeTimeout = Integer.parseInt(props.getProperty("upgradeTimeout"));
        int pingTimeout = Integer.parseInt(props.getProperty("pingTimeout"));
        int pingInterval = Integer.parseInt(props.getProperty("pingInterval"));

        config.setSocketConfig(socketConfig);
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(bossCount);
        config.setWorkerThreads(workCount);
        config.setAllowCustomRequests(allowCustomRequests);
        config.setUpgradeTimeout(upgradeTimeout);
        config.setPingTimeout(pingTimeout);
        config.setPingInterval(pingInterval);
        config.setTransports(Transport.WEBSOCKET);
        in.close();

        threadPool = new ThreadPoolExecutor(
                0, workCount,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("Thread-for-FutureTask-%d").build()
        );
        server = new SocketIOServer(config);

        server.addConnectListener(client -> {
            String agentID = Commons.getAgentID(client);
            String dbName = Commons.getDBName(client);
            if(agentID == null){
                // 如果连接信息错误，发送异常信息，关闭socket
                logger.info("连接信息错误：agentID, 连接关闭");
                client.disconnect();
            }
            // 缓存连接
            ClientCache.saveClient(agentID, dbName, client);
        });

        // 添加客户端连接监听器
        server.addDisconnectListener(client -> {
            String agentID = Commons.getAgentID(client);
            String dbName = Commons.getDBName(client);

            if(agentID == null){
                // 如果连接信息错误，发送异常信息，关闭socket
                logger.info("agentID: 连接关闭");
                client.disconnect();
            }

            // 缓存连接
            ClientCache.deleteClient(agentID, dbName);
            logger.info("agentID: " + agentID + "连接关闭");
            logger.info("agentID: " + agentID + "连接已删除");
        });

        server.addEventListener("message", String.class,
                ((client, data, ackRequest) -> logger.info("message: " + data)
        ));
    }
}


package com.fanruan;

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.fanruan.cache.ClientCache;
import com.fanruan.cache.ClientState;
import com.fanruan.exception.ParamException;
import com.fanruan.pojo.MyDataSource;
import com.fanruan.pojo.message.SimpleMessage;
import com.fanruan.utils.CodeMsg;
import com.fanruan.utils.GlobalExceptionHandler;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class ServerStater{
    protected static final Logger logger = LogManager.getLogger();

    public static final Gson gson = new Gson();

    public static SocketIOServer server;

    public static ClientCache cache;

    public ServerStater() throws IOException {
        registerClient();
        loadConfig();
        bootStrap();
    }

    private void bootStrap(){
        logger.debug("配置事件监听");
        server.addConnectListener(client -> {
            String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
            if(agentID == null){
                // 如果连接信息错误，发送异常信息，关闭socket
                GlobalExceptionHandler.sendException(client, new ParamException(CodeMsg.CLIENT_ID_ERROR));
                logger.info("连接信息错误：agentID, 连接关闭");
                client.disconnect();
            }
            // 缓存连接
            cache.saveClient(agentID, client);

            // 设置缓存状态为未注册
            cache.getStateByID(agentID).setState(ClientState.STATE_UNREGISTER);
            logger.info("agentID " + agentID + " 连接建立成功");
            logger.info("请求代理注册数据源");
            client.sendEvent("ClientReceive", new SimpleMessage("Ask for DB registration"));
        });

        // 添加客户端连接监听器
        server.addDisconnectListener(client -> {
            String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
            if(agentID == null){
                // 如果连接信息错误，发送异常信息，关闭socket
                GlobalExceptionHandler.sendException(client, new ParamException(CodeMsg.CLIENT_ID_ERROR));
                logger.info("agentID: 连接关闭");
                client.disconnect();
            }
            // 缓存连接
            cache.deleteAgentByID(agentID);
            logger.info("agentID: " + agentID + "连接关闭");
            logger.info("agentId: " + agentID + "连接已删除");
        });

        // 处理错误事件
        server.addEventListener("ErrorMessage", SimpleMessage.class, ((client, data, ackRequest) -> {
            logger.info("Error: " + data.getMsg());
        }));

        // 客户端准备接受数据源注册时返回，事件发生时，用户的开始进行事件注册
        server.addEventListener("DataSource", String.class, ((client, data, ackRequest) -> {
            String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
            logger.info("DataSource Event: " + data);
            MyDataSource dataSource = cache.getDataSourceByID(agentID);
            String msg = gson.toJson(dataSource);
            client.sendEvent("DataSource", msg);
        }));

        server.addEventListener("DataSourceReady", String.class, ((client, data, ackRequest) -> {
            String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
            // 数据源注册完成设置连接状态为完成
            cache.getStateByID(agentID).setState(ClientState.STATE_COMPLETE);
            logger.info("DataSourceReady: " + data);
        }));

        server.addEventListener("ReturnData", String.class, ((client, data, ackRequest) -> {
            String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
            logger.info(data);
            // 数据源注册完成设置连接状态为完成
        }));

        server.start();
    }

    private void registerClient(){
        cache = new ClientCache();
        MyDataSource dataSource = new MyDataSource(
                "mysql",
                "jdbc:mysql://127.0.0.1:3306/test",
                "root",
                "850656"
        );
        cache.init("1001", dataSource);
    }

    private void loadConfig() throws IOException {
        logger.debug("加载配置");
        SocketConfig socketConfig = new SocketConfig();
        // 是否开启 Nagle 算法
        socketConfig.setTcpNoDelay(true);

        com.corundumstudio.socketio.Configuration config =
                new com.corundumstudio.socketio.Configuration();

        InputStream in = this.getClass().getResourceAsStream("/socketIO.properties");
        Properties props = new Properties();
        InputStreamReader inputStreamReader = new InputStreamReader(in, "UTF-8");
        props.load(inputStreamReader);

        config.setSocketConfig(socketConfig);
        config.setHostname(props.getProperty("host"));
        logger.debug("properties_host: " + props.getProperty("host"));
        config.setPort(Integer.parseInt(props.getProperty("port")));
        config.setBossThreads(Integer.parseInt(props.getProperty("bossCount")));
        config.setWorkerThreads(Integer.parseInt(props.getProperty("workCount")));
        config.setAllowCustomRequests(Boolean.parseBoolean(props.getProperty("allowCustomRequests")));
        config.setUpgradeTimeout(Integer.parseInt(props.getProperty("upgradeTimeout")));
        config.setPingTimeout(Integer.parseInt(props.getProperty("pingTimeout")));
        config.setPingInterval(Integer.parseInt(props.getProperty("pingInterval")));
        config.setTransports(Transport.WEBSOCKET);
        in.close();

        server = new SocketIOServer(config);
        cache = new ClientCache();
    }
}


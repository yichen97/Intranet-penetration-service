package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.pojo.MyDataSource;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ClientCache {

    //    缓存代理客户端信息
    private static Map<String, ClientWrapper> map = new ConcurrentHashMap<>();

    //  before service established link with agent, service should set cache for agent you want to accept.
//    public static void init(String agentID, MyDataSource dataSource){
//        map.put(agentID, new ClientWrapper(null, null, dataSource, null));
//    }
    /**
     * 缓存代理通道
     * @param agentID 代理名称
     * @param client 代理通道
     */
    public static void saveClient(String agentID, String dbName, SocketIOClient client){
        ClientWrapper wrapper = map.getOrDefault(agentID, new ClientWrapper());
        wrapper.setState(new ClientState());
        wrapper.saveClient(dbName, client);
        map.put(agentID, wrapper);
    }

    /**
     * 删除缓存的代理
     * @param agentID 代理名称
     */
    public static void deleteAgentByID(String agentID){
        if(map.containsKey(agentID)) {
            map.get(agentID);
        }
    }


    public static SocketIOClient getClient(String agentID, String dbName){
        // 使用异步任务等待连接
        if(map.containsKey(agentID)) {
            return map.get(agentID).getClient(dbName);
        }else{
            return null;
        }
    }

    public static ClientState getStateByID(String agentID){
        if(map.containsKey(agentID)) {
            return map.get(agentID).getState();
        }else{
            return null;
        }
    }


    public static ClientWrapper getClientWrapperByID(String agentID){
        return map.getOrDefault(agentID, new ClientWrapper());
    }
}
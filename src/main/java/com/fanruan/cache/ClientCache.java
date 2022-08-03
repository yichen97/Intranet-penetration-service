package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.pojo.MyDataSource;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ClientCache {

    //    缓存代理客户端信息
    private static Map<String, Map<String, ClientWrapper>> agentMap = new ConcurrentHashMap<>();

    /**
     * 缓存代理通道
     * @param agentID 代理名称
     * @param client 代理通道
     */
    public static void saveClient(String agentID, String dbName, SocketIOClient client){
        Map<String, ClientWrapper> nameSpaceMap = agentMap.get(agentID);
        if(nameSpaceMap == null){
            nameSpaceMap = new ConcurrentHashMap<>();
            agentMap.put(agentID, nameSpaceMap);
        }
        ClientWrapper wrapper = nameSpaceMap.get(dbName);
        if(wrapper == null){
            wrapper = new ClientWrapper();
            nameSpaceMap.put(dbName, wrapper);
        }
        wrapper.setClient(client);
    }

    public static SocketIOClient getClient(String agentID, String dbName){
        Map<String, ClientWrapper> map = agentMap.get(agentID);
        if(map == null) return null;
        ClientWrapper wrapper = map.get(dbName);
        if(wrapper == null) return null;
        return wrapper.getClient();
    }

    public static void deleteClient(String agentID, String dbName){
        Map<String, ClientWrapper> map = agentMap.get(agentID);
        if(map == null) return;
        map.remove(dbName);

    }


    public static ClientWrapper getClientWrapper(String agentID, String dbName){
        Map<String, ClientWrapper> map = agentMap.get(agentID);
        if(map == null) throw new RuntimeException("wrong agent ID");
        ClientWrapper wrapper = map.get(dbName);
        if(wrapper == null) throw new RuntimeException("wrong dbName");
        return wrapper;
    }
}
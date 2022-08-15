package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Yichen Dai
 */
public class ClientCache {

    private static final Map<String, Map<String, ClientWrapper>> AGENT_MAP = new ConcurrentHashMap<>();

    /**
     * 缓存代理通道
     * @param agentID 代理名称
     * @param client 代理通道
     */
    public static void saveClient(String agentID, String dbName, SocketIOClient client){
        Map<String, ClientWrapper> nameSpaceMap = AGENT_MAP.get(agentID);
        if(nameSpaceMap == null){
            nameSpaceMap = new ConcurrentHashMap<>(ServerStater.workCount * 10);
            AGENT_MAP.put(agentID, nameSpaceMap);
        }
        ClientWrapper wrapper = nameSpaceMap.get(dbName);
        if(wrapper == null){
            wrapper = new ClientWrapper();
            nameSpaceMap.put(dbName, wrapper);
        }
        wrapper.setClient(client);
    }

    public static SocketIOClient getClient(String agentID, String dbName){
        Map<String, ClientWrapper> map = AGENT_MAP.get(agentID);
        if(map == null) {
            return null;
        }
        ClientWrapper wrapper = map.get(dbName);
        if(wrapper == null) {
            return null;
        }
        return wrapper.getClient();
    }

    public static void deleteClient(String agentID, String dbName){
        Map<String, ClientWrapper> map = AGENT_MAP.get(agentID);
        if(map == null) {
            return;
        }
        map.remove(dbName);

    }


    public static ClientWrapper getClientWrapper(String agentID, String dbName){
        Map<String, ClientWrapper> map = AGENT_MAP.get(agentID);
        if(map == null) {
            throw new RuntimeException("wrong agent ID");
        }
        ClientWrapper wrapper = map.get(dbName);
        if(wrapper == null) {
            throw new RuntimeException("wrong dbName");
        }
        return wrapper;
    }
}
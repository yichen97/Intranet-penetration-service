package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.pojo.MyDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCache {

    //    缓存代理客户端信息
    private static Map<String, ClientWrapper> map = new ConcurrentHashMap<>();


    /**
     * 缓存代理通道
     * @param agentID 代理名称
     * @param client 代理通道
     */
    public void saveClient(String agentID, SocketIOClient client){
        ClientWrapper wrapper = map.get(agentID);
        wrapper.setClient(client);
        wrapper.setState(new ClientState());
    }

    /**
     * 删除缓存的代理
     * @param agentID 代理名称
     */
    public void deleteAgentByID(String agentID){
        if(map.containsKey(agentID)) {
            map.get(agentID);
        }
    }


    public SocketIOClient getClientByID(String agentID){
        // 使用异步任务等待连接
        if(map.containsKey(agentID)) {
            return map.get(agentID).getClient();
        }else{
            return null;
        }
    }

    public ClientState getStateByID(String agentID){
        if(map.containsKey(agentID)) {
            return map.get(agentID).getState();
        }else{
            return null;
        }
    }

    public MyDataSource getDataSourceByID(String agentID){
        if(map.containsKey(agentID)) {
            return map.get(agentID).getDataSource();
        }else{
            return null;
        }
    }

    public ResultSet getResultSetID(String agentID){
        if(map.containsKey(agentID)) {
            return map.get(agentID).getRs();
        }else{
            return null;
        }
    }

    public void setResultSetID(String agentID, ResultSet resultSet){
        map.get(agentID).setRs(resultSet);
    }

    public void init(String agentID, MyDataSource dataSource){
        map.put(agentID, new ClientWrapper(null, null, dataSource, null));
    }

    @Data
    @AllArgsConstructor
    class ClientWrapper{
        private SocketIOClient client;
        private volatile ClientState state;
        private MyDataSource dataSource;
        private volatile ResultSet rs;
    }

}
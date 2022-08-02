package com.fanruan.utils;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.cache.ClientCache;

public class Commons {

    static public String getAgentID(SocketIOClient client){
        String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
        return agentID;
    }
}

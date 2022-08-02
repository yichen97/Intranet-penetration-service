package com.fanruan.utils;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;

public class GlobalExceptionHandler {

    static ClientCache cache = ServerStater.cache;

    static public void sendException(SocketIOClient client, Exception e){
        client.sendEvent("ErrorMessage", e.getMessage());
    }

    static public void sendException(SocketIOClient client, String errMsg){
        client.sendEvent("ErrorMessage", errMsg);
    }

    static public void sendException(String agentID, String errMsg){

    }
}

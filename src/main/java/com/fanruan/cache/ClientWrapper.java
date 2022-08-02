package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientWrapper {
        private ClientState state;
        private static Map<String, SocketIOClient> socketCache = new ConcurrentHashMap<>();
        final public ReentrantLock lock = new ReentrantLock();
        final public Condition condition = lock.newCondition();

        public SocketIOClient getClient(String dbName){
                SocketIOClient client = socketCache.get(dbName);
                if(client == null) throw new RuntimeException("no such client");
                return client;
        }

        public void saveClient(String dbName, SocketIOClient client){
                socketCache.put(dbName, client);
        }
}

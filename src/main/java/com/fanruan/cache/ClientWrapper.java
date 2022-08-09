package com.fanruan.cache;

import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.pojo.message.RpcRequest;
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

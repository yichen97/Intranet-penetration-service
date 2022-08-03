package com.fanruan.utils;

import com.corundumstudio.socketio.SocketIOClient;

import java.util.Calendar;
import java.util.Random;


public class Commons {

    static public String getAgentID(SocketIOClient client){
        String agentID = client.getHandshakeData().getSingleUrlParam("agentID");
        return agentID;
    }

    static public String getDBName(SocketIOClient client){
        // spaceNamed 的格式为 "/" + "dbName"
        // default name space named as "/"
        String spaceName = client.getNamespace().getName();
        String dbName;
        if(spaceName.length() > 1) {
            dbName = spaceName.substring(1);
        }else{
            dbName = spaceName;
        }
        return dbName;
    }

    public static String getID(){
        return getTimeInMillis() + getRandom();
    }

    public static String getTimeInMillis() {
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        return timeInMillis+"";
    }

    public static String getRandom() {
        Random random = new Random();
        int nextInt = random.nextInt(9000000);
        nextInt=nextInt+1000000;
        String str=nextInt+"";
        return str;
    }

}

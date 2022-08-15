package com.fanruan.utils;

import com.corundumstudio.socketio.SocketIOClient;

import java.util.Calendar;
import java.util.Random;


public class Commons {

    static public String getAgentID(SocketIOClient client){
        return client.getHandshakeData().getSingleUrlParam("agentID");
    }

    static public String getDBName(SocketIOClient client){
        // the format of spaceNamed  "/" + "dbName"
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
        nextInt = nextInt + 1000000;
        return nextInt+"";
    }

}

package com.fanruan.jdbc.driver;

import com.fanruan.jdbc.connection.MyConnection;
import com.fanruan.proxy.ProxyFactory;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * @author Yichen Dai
 */
public class MyDriver implements Driver {

    static public final int DRIVER_VERSION_MAJOR = 1;
    static public final int DRIVER_VERSION_MINOR = 1;
    private String ID;

    //依靠静态函数块注册驱动
    static{
        try {
            DriverManager.registerDriver((MyDriver) ProxyFactory.getProxy(MyDriver.class, null));
        } catch (Exception e) {
            throw new RuntimeException("Can't register driver");
        }
    }


    /**
     *   These corresponding code is to make the format correct, because the getID()
     *   will be called, even if the filed is never not null.
     * @return ID
     */
    public String getID(){
        return this.ID;
    }

    public void setID(String ID){
        this.ID = ID;
    }

    @Override
    public Connection connect(String url, Properties info){
        String dbName = info.getProperty("agentDBName");
        if(dbName == null){
            dbName = url.split(":")[1];
            info.setProperty("agentDBName", dbName);
        }
        MyConnection myConn = (MyConnection) ProxyFactory.getProxy(MyConnection.class, info);
        myConn.setInfo(info);
        return myConn;
    }

    @Override
    public boolean acceptsURL(String url){
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info){
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return DRIVER_VERSION_MAJOR;
    }

    @Override
    public int getMinorVersion() {
        return DRIVER_VERSION_MINOR;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}

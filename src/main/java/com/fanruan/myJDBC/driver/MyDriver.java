package com.fanruan.myJDBC.driver;

import com.corundumstudio.socketio.SocketIOClient;

import com.fanruan.ServerStater;
import com.fanruan.cache.ClientCache;
import com.fanruan.myJDBC.connection.MyConnection;
import lombok.SneakyThrows;



import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;


public class MyDriver implements Driver {

    private ClientCache cache = ServerStater.cache;

    SocketIOClient client = null;
    static public final int DRIVER_VERSION_MAJOR = 1;
    static public final int DRIVER_VERSION_MINOR = 1;

    //依靠静态函数块注册驱动
    static{
        try {
            DriverManager.registerDriver(new MyDriver());
        } catch (Exception e) {
            throw new RuntimeException("Can't register driver");
        }
    }

    @SneakyThrows
    @Override
    public Connection connect(String AgentID, Properties info) throws SQLException {
        this.client = cache.getClientByID(AgentID);
        MyConnection conn = new MyConnection(client);
        return (Connection) conn;
    }

    // URL的正确性交给Agent验证
    // 后续需要完善，通过Agent返回的错误消息进行更新
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
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

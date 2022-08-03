import com.fanruan.ServerStater;


import com.fanruan.utils.DBProperties;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;


public class Test {
    protected static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        String[] DBs = new String[]{
                DBProperties.MYSQL,
//                DBProperties.POSTGRESQL,
//                DBProperties.SQLSERVER,
//                DBProperties.DB2,
//                DBProperties.ORACLE
        };
        ServerStater serverStater = new ServerStater(DBs);

        String agentID = "1001";

        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while(serverStater.cache.getClient("1001", "mysql") == null){
                    Thread.sleep(1000);
                }
                Properties info = new Properties();
                info.setProperty("user", "root");
                info.setProperty("password", "850656");
                info.setProperty("agentID", "1001");
                info.setProperty("dbName", "mysql");

                Class.forName("com.fanruan.myJDBC.driver.MyDriver");
                Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test", info);
                Statement st = conn.createStatement();
//                st.executeQuery("select * from `student`");

            }
        }).start();
    }
}

import com.fanruan.ServerStater;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

        ServerStater serverStater = new ServerStater();
        String agentID = "1001";
        Class.forName("com.fanruan.myJDBC.driver.MyDriver");

        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while(serverStater.cache.getClientByID(agentID) == null){
                    logger.info("等待与 Agent: " + agentID + "建立连接");
                    Thread.sleep(1000);
                }
                Connection conn = DriverManager.getConnection("1001", new Properties());
                Statement st = conn.createStatement();
                st.executeQuery("select * from `student`");
            }
        }).start();
        System.in.read();

    }
}

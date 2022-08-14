import com.fanruan.ServerStater;


import com.fanruan.utils.DBProperties;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.nio.ch.ThreadPool;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Test {
    protected static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {

        Test test = new Test();

        String[] DBs = new String[]{
                DBProperties.MYSQL,
                DBProperties.POSTGRESQL,
//                DBProperties.SQLSERVER,
//                DBProperties.DB2,
//                DBProperties.ORACLE
        };
        ServerStater serverStater = new ServerStater(DBs);

        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        test.testPostSQL(serverStater, threadPool);
        test.testMySQL(serverStater, threadPool);

    }

    public void testMySQL(ServerStater serverStater, ExecutorService threadPool){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(serverStater.cache.getClient("1001", "mysql") == null){
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Properties info = new Properties();
                info.setProperty("user", "root");
                info.setProperty("password", "850656");
                info.setProperty("agentID", "1001");
                info.setProperty("agentDBName", "mysql");

                Connection conn = null;
                Statement st = null;
                PreparedStatement pst = null;
                PreparedStatement pst2 = null;
                ResultSet rs1 = null;
                ResultSet rs2 = null;
                ResultSet rs3 = null;
                try {
                    Class.forName("com.fanruan.myJDBC.driver.MyDriver");
                    conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test", info);
                    st = conn.createStatement();
                    rs1 = st.executeQuery("select * from `student`");

                    System.out.println("-----------");
                    System.out.println("执行查询语句");
                    while(rs1.next()) {
                        System.out.print(rs1.getInt("student_id") + "  ");
                        System.out.print(rs1.getString("student_name")+ "  ");
                        System.out.println(rs1.getString("student_address")+ "  ");
                    }

                    String sql = "select * from `student` where `student_name`= ?";
                    pst = conn.prepareStatement(sql);
                    pst.setString(1, "张三");
                    rs2 = pst.executeQuery();

                    System.out.println("-----------");
                    System.out.println("执行预查询语句1");
                    while(rs2.next()) {
                        System.out.print(rs2.getInt("student_id") + "  ");
                        System.out.print(rs2.getString("student_name")+ "  ");
                        System.out.println(rs2.getString("student_address")+ "  ");
                    }

                    sql = "select * from `student` where `student_address`= ?";
                    pst2 = conn.prepareStatement(sql);
                    pst2.setString(1, "上海");
                    rs3 = pst2.executeQuery();

                    System.out.println("-----------");
                    System.out.println("执行预查询语句2");
                    while(rs3.next()) {
                        System.out.print(rs3.getInt("student_id") + "  ");
                        System.out.print(rs3.getString("student_name")+ "  ");
                        System.out.println(rs3.getString("student_address")+ "  ");
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 7、关闭对象，回收数据库资源
                    if (rs1 != null) { //关闭结果集对象
                        try {
                            rs1.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (rs2 != null) { //关闭结果集对象
                        try {
                            rs2.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (st != null) { // 关闭数据库操作对象
                        try {
                            st.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (pst != null) { // 关闭数据库操作对象
                        try {
                            pst.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null) { // 关闭数据库连接对象
                        try {
                            if (!conn.isClosed()) {
                                conn.close();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        threadPool.execute(thread);
    }

    public void testPostSQL(ServerStater serverStater, ExecutorService threadPool){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(serverStater.cache.getClient("1001", "postgresql") == null){
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Properties info = new Properties();
                info.setProperty("user", "postgres");
                info.setProperty("password", "850656");
                info.setProperty("agentID", "1001");
                info.setProperty("agentDBName", "postgresql");

                Connection conn = null;
                Statement st = null;
                PreparedStatement pst = null;
                PreparedStatement pst2 = null;
                ResultSet rs1 = null;
                ResultSet rs2 = null;
                ResultSet rs3 = null;
                try {
                    Class.forName("com.fanruan.myJDBC.driver.MyDriver");
                    conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/test", info);
                    st = conn.createStatement();
                    rs1 = st.executeQuery("select * from student");

                    System.out.println("-----------");
                    System.out.println("执行查询语句");
                    while(rs1.next()) {
                        System.out.print(rs1.getInt("student_id") + "  ");
                        System.out.print(rs1.getString("student_name")+ "  ");
                        System.out.println(rs1.getString("student_address")+ "  ");
                    }

                    String sql = "select * from student where student_name= ?";
                    pst = conn.prepareStatement(sql);
                    pst.setString(1, "张三");
                    rs2 = pst.executeQuery();

                    System.out.println("-----------");
                    System.out.println("执行预查询语句1");
                    while(rs2.next()) {
                        System.out.print(rs2.getInt("student_id") + "  ");
                        System.out.print(rs2.getString("student_name")+ "  ");
                        System.out.println(rs2.getString("student_address")+ "  ");
                    }

                    sql = "select * from student where student_address = ?";
                    pst2 = conn.prepareStatement(sql);
                    pst2.setString(1, "上海");
                    rs3 = pst2.executeQuery();

                    System.out.println("-----------");
                    System.out.println("执行预查询语句2");
                    while(rs3.next()) {
                        System.out.print(rs3.getInt("student_id") + "  ");
                        System.out.print(rs3.getString("student_name")+ "  ");
                        System.out.println(rs3.getString("student_address")+ "  ");
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 7、关闭对象，回收数据库资源
                    if (rs1 != null) { //关闭结果集对象
                        try {
                            rs1.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (rs2 != null) { //关闭结果集对象
                        try {
                            rs2.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (st != null) { // 关闭数据库操作对象
                        try {
                            st.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (pst != null) { // 关闭数据库操作对象
                        try {
                            pst.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null) { // 关闭数据库连接对象
                        try {
                            if (!conn.isClosed()) {
                                conn.close();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        threadPool.execute(thread);
    }
}

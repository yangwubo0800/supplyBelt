package com.power.supplybelt.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DMUtils {

    // 定义 DM JDBC 驱动串
    String jdbcString = "dm.jdbc.driver.DmDriver";
    // 定义连接对象
    Connection conn = null;
    
    
    // 定义 DM URL 连接串
    String urlString = "jdbc:dm://192.168.65.41:5236";
    // 定义连接用户名
    String userName = "D5000";
    // 定义连接用户口令
    String password = "123456789";

    // 定义 DM URL 连接串--lyq
//    String urlString = "jdbc:dm://192.168.65.241:5236";
    
//    String urlString = "jdbc:dm://192.168.65.98:5236";
//    
//    // 定义连接用户名
//    String userName = "D5000";
//    // 定义连接用户口令
//    String password = "D5000";



    // 定义 DM URL 连接串--hf
//    String urlString = "jdbc:dm://192.168.65.213:5236";
//    // 定义连接用户名
//    String userName = "D5000";
//    // 定义连接用户口令
//    String password = "123456789";
    
    




    /* 加载 JDBC 驱动程序
     * @throws SQLException 异常 */
    public void loadJdbcDriver() throws SQLException {
        try {
            System.out.println("Loading JDBC Driver...");
            // 加载 JDBC 驱动程序
            Class.forName(jdbcString);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Load JDBC Driver Error : " + e.getMessage());
        } catch (Exception ex) {
            throw new SQLException("Load JDBC Driver Error : "
                    + ex.getMessage());
        }
    }


    /* 连接 DM 数据库
     * @throws SQLException 异常 */
    public void connect() throws SQLException {
        try {
            System.out.println("Connecting to DM Server...");
            // 连接 DM 数据库
            conn = DriverManager.getConnection(urlString, userName, password);
        } catch (SQLException e) {
            throw new SQLException("Connect to DM Server Error : "
                    + e.getMessage());
        }
    }
    /* 关闭连接
     * @throws SQLException 异常 */
    public void disConnect() throws SQLException {
        try {
            // 关闭连接
            conn.close();
        } catch (SQLException e) {
            throw new SQLException("close connection error : " + e.getMessage());
        }
    }


    public Connection getConnection(){
        if (null != conn){
            return conn;
        }else {
            try {
                loadJdbcDriver();
                connect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return  conn;
    }
}

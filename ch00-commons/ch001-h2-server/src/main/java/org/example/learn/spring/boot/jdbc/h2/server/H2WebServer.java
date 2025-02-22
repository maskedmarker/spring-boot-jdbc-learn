package org.example.learn.spring.boot.jdbc.h2.server;

import org.h2.server.web.WebServer;
import org.h2.tools.Server;

import java.sql.SQLException;

/**
 * WebServer不仅启动h2数据库实例,还启动了console管理台
 *
 * WebServer启动了WebApp,用来处理console页面的请求
 * console页面的请求包括执行数据库脚本,这些请求被WebServer接收到之后,
 * 提取http报文中的sql脚本,然后操作java.sql.Connection执行sql脚本
 *
 * WebServer模式下,外部进程是无法访问jdbc连接的
 */
public class H2WebServer {

    public static void main(String[] args) {
        try {
//            start0(args);
            start1(args);
        } catch (Exception e) {
            System.err.println("Error starting H2 server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void start0(String[] args) throws SQLException {
        // -ifNotExists 当数据库不存在时,自动创建
        Server h2WebServer = Server.createWebServer("-webPort", "8082", "-ifNotExists", "-trace");
        h2WebServer.start();
        System.out.println("H2 Database Server started and running at: " + h2WebServer.getURL());
    }

    private static void start1(String[] args) throws SQLException {
        WebServer service = new WebServer();
        service.setKey(null);
        service.setAllowSecureCreation(false);
        Server server = new Server(service, "-webPort", "8082", "-ifNotExists");
        service.setShutdownHandler(server);
        server.start();

        System.out.println("H2 Database Server started and running at: " + server.getURL());
    }
}

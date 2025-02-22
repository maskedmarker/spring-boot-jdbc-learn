package org.example.learn.spring.boot.jdbc.h2.server;

import org.h2.server.TcpServer;
import org.h2.tools.Server;

/**
 * org.h2.tools.Server是h2的启动类,这里简单wrap一层是为说明可以通过编程方式独立启动h2
 *
 * TcpServer监听jdbc报文,然后操作Driver执行sql
 */
public class H2TcpServer {

    public static void main(String[] args) {
        try {
            // Start H2 in TCP Server mode
            TcpServer service = new TcpServer();
            Server tcpServer = new Server(service, "-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists", "-trace");
            service.setShutdownHandler(tcpServer);
            tcpServer.start();
            System.out.println("H2 Database Server started and running at: " + tcpServer.getURL());
        } catch (Exception e) {
            System.err.println("Error starting H2 server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

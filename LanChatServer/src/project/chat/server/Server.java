package project.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Server {
    //定义一个集合，用于存储所有Socket
    //定义一个map集合，键用于存储socket，值存储用户名
    public static final Map<Socket,String> onLineSocket = new HashMap<>();
    public static void main(String[] args) throws IOException {
        // 1. 获取当前日期和时间
        LocalDateTime now = LocalDateTime.now();

        // 2. 定义格式化模板 (yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("启动服务器端");
        try {
            //启动服务器端
            ServerSocket serverSocket = new ServerSocket(Constant.Server_port);
            //主线程负责接收客户端的请求
            while(true){
                //调用accept方法，接收客户端的Socket对象
                System.out.println("等待客户端连接...");
                String formattedDate = now.format(formatter);
                System.out.println("当前本机时间：" + formattedDate);
                //创建一个线程，处理客户端的请求
                Socket socket = serverSocket.accept();
                //下面代码等同于
                //Thread x = new Thread(new ServerReaderThread(socket));
                //x.start();
                new ServerReaderThread(socket).start();

                System.out.println("一个客户端连接了");
                System.out.println("当前本机时间：" + formattedDate);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

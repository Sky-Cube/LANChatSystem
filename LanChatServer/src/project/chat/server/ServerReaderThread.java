package project.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

public class ServerReaderThread extends Thread{
    private final Socket socket;

    public ServerReaderThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(is);

            while (true) {
                int type = dis.readInt();
                switch(type){
                    case 1:
                        String username = dis.readUTF();
                        Server.onLineSocket.put(socket,username);
                        updateClientOnLineList();
                        break;

                    case 2:
                        String msg = dis.readUTF();
                        sendMsgToAll(msg);
                        break;

                    case 3:
                        // 【关键新增】真正私聊转发
                        String fromUser = dis.readUTF();
                        String toUser = dis.readUTF();
                        String privateMsg = dis.readUTF();

                        sendPrivateMsg(fromUser,toUser,privateMsg); // 【新增】
                        break;
                }
            }
        } catch (IOException e) {
            Server.onLineSocket.remove(socket);
        }
    }



    //更新在线列表

    private void updateClientOnLineList() throws IOException {
        //更新人数列表，获取在线客户端用户名称，并转发全部在线的socket管道
        //获取当前全部在线用户名称
        Collection<String> values = Server.onLineSocket.values();
        //推送给全部客户端
        for(Socket socket : Server.onLineSocket.keySet()){
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(1);//1.代表告诉客户端下面是更新人数列表 2.代表发送群聊消息
            try {
                //告诉客户端在线人数
                dos.writeInt(values.size());//values代表存储的在线人数管道
                //遍历socket集合中的用户名，遍历一个推送给客户端一个
                for(String value : values){
                    dos.writeUTF(value);
                    System.out.println("更新在线列表："+value);
                }
                dos.flush();//数据刷新
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //转发群聊消息
    private void sendMsgToAll(String msg) {
        //拼接转发消息 ：用户名时间 消息内容 使用StringBuilder()效率高，最后还是转为String
        //1.创建一个StringBuilder()对象
        StringBuilder sb = new StringBuilder();
        //2.获取当前线程用户名
        String name = Server.onLineSocket.get(socket);
        //3.获取当前时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");//格式化时间消息
        String nowStr = dtf.format(now);
        //4.拼接消息
        String msgResult = sb.append(name).append(":").append(nowStr).append(":").append("\r\n").append(msg).append("\r\n").toString();
        //5.推送给全部在线客户端
        for(Socket socket : Server.onLineSocket.keySet()){

            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeInt(2);//1.代表告诉客户端下面是更新人数列表 2.代表发送群聊消息
                dos.writeUTF(msgResult);
                dos.flush();//数据刷新
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    // 私聊转发实现
    private void sendPrivateMsg(String fromUser, String toUser, String msg) throws IOException {
        for (Map.Entry<Socket,String> entry : Server.onLineSocket.entrySet()) {
            if (entry.getValue().equals(toUser)) {
                DataOutputStream dos = new DataOutputStream(entry.getKey().getOutputStream());
                dos.writeInt(3);
                dos.writeUTF(fromUser);
                dos.writeUTF(toUser);
                dos.writeUTF(msg);
                dos.flush();
            }
        }
    }
}




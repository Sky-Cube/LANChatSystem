package project.chat.ui;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientReaderThread extends Thread{
    private Socket socket;
    private DataInputStream dis;
    private ClientChatFrame windows;
    public ClientReaderThread(Socket socket, ClientChatFrame windows){
        this.windows = windows;
        this.socket = socket;
    }
    //以下代码中的socket代表的是每一个线程的 socket，互不干扰
    @Override
    public void run(){
        //获取输入流信息，读取客户端数据
        try {
            //接收消息的客户端需要接收多种消息 1.代表在线人数更新 2.代表群聊数据
            this.dis = new DataInputStream(socket.getInputStream());
            //等同于 DataInputStream dis = new DataInputStream(socket.getInputStream());
            //接收消息类型
            while (true) {
                int type = dis.readInt();
                switch(type){
                    case 1:
                        //在线人数
                        //服务端发送的在线人数数据
                        updateClientOnLineList(dis);
                        // System.out.println("用户"+nick+"已登录");
                        break;
                    case 2:
                        //群聊消息
                        //服务端发送来的群聊消息
                        getMsgToClient(dis);
                        //System.out.println("群聊消息："+msg);
                        break;
                    case 3:
                         //私聊消息
                         //服务端发送来的私聊消息
                         getPrivateMsgToClient(dis);
                         break;

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    //更新客户端在线列表
    private void updateClientOnLineList(DataInputStream dis) throws IOException {
        //更新人数列表，获取在线客户端用户名称，并转发全部在线的socket管道
        //1.获取当前全部在线用户名称
        int count = this.dis.readInt();
        //2.循环控制读取多少个用户信息
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            //读取每个用户信息
            String name = this.dis.readUTF();
            //将信息添加到集合中
            names[i] = name;

        }
        //3.更新窗口在线人数数据
        windows.updateOnLineList(names);
    }

    //群聊消息
    private void getMsgToClient(DataInputStream dis) throws IOException {
        //获取群聊消息
        //与服务端线程处理的 dos.writeUTF(msgResult);这段代码相对应
        String msg = dis.readUTF();
        windows.setMsgToClient(msg);
    }

    //私聊消息
    private void getPrivateMsgToClient(DataInputStream dis) throws IOException {
        //获取群聊消息
        //与服务端线程处理的 dos.writeUTF(msgResult);这段代码相对应
        String name = dis.readUTF();
        String targetUser = dis.readUTF();
        String PrivateMsg = dis.readUTF();
        windows.setPrivateMsgToClient(name,targetUser,PrivateMsg);
    }

}

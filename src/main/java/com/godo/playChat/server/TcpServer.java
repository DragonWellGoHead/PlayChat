package com.godo.playChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.godo.playChat.common.utils.AESUtils;
import com.godo.playChat.common.utils.Base64Utils;
import com.godo.playChat.common.utils.RSAUtils;
import com.godo.playChat.common.utils.message.ChatMess;
import com.godo.playChat.common.utils.message.ExchangeMess;
import com.godo.playChat.common.utils.socket.SecSocket;

/**
 * 
 * @ClassName:  TcpServer   
 * @Description: 创建服务端对象  
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午11:34:40
 */
@Service("tcpService")
public class TcpServer {

    /** 已经成功登录的客户端列表 **/
    private Map<String, Client> clientList = new ConcurrentHashMap<String, Client>();
    /** 客户端登录默认密码  **/
    @Value("${tcpServer.defaultPwd}")
    private String defaultPwd = "gomove";
    /** 客户端和服务端消息通信的端口**/
    @Value("${tcpServer.serverPort}")
    private int serverPort = 3434;
    /** 客户端和服务端文件传输的端口**/
    @Value("${tcpServer.filePort}")
    private int serverFilePort = 3535;
    /** 服务端存储离线文件的目录**/
    @Value("${tcpServer.baseStorePath}")
    private String baseStorePath = "./filestore";
    /** 处理用户连接的最大线程数 **/
    @Value("${tcpServer.threadPool.maxThreads}")
    private int maxThreads = 5;
    /** 刷新用户状态的周期 **/
    @Value("${tcpServer.refreshInterval}")
    private int refreshInterval = 5;
    /** 最大登录用户数 **/
    @Value("${tcpServer.maxUsers}")
    private int maxUsers = 100;

    /** 服务端密钥对 **/
    private final Map<String, Object> keyPair = RSAUtils.genKeyPair();
    /** 服务端公钥 **/
    private final String serverPublicKey = RSAUtils.getPublicKey(keyPair);
    /** 服务端私钥 **/
    private final String serverPrivateKey = RSAUtils.getPrivateKey(keyPair);

    /**
     * 
     * @ClassName:  Client   
     * @Description: 客户端对象
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:55:05
     */
    private class Client {

        private String name;
        private String shareKey;
        private String storePath;
        private int state = 0;
        private Date loginTime;
        private Date logoutTime;
        private SecSocket<ChatMess> sSocket;
        private SecSocket<ChatMess> sFileSocket;
        private Queue<ChatMess> msgQueue = new LinkedList<ChatMess>();// 消息队列，用于保存离线消息。

        public Client(String name, String shareKey, SecSocket<ChatMess> socket) {
            this.name = name;
            this.shareKey = shareKey;
            this.sSocket = socket;
        }

        public String getName() {
            return name;
        }

        public String getShareKey() {
            return shareKey;
        }

        public SecSocket<ChatMess> getsSocket() {
            return sSocket;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setShareKey(String shareKey) {
            this.shareKey = shareKey;
        }

        public void setsSocket(SecSocket<ChatMess> sSocket) {
            this.sSocket = sSocket;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public Date getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(Date loginTime) {
            this.loginTime = loginTime;
        }

        public Date getLogoutTime() {
            return logoutTime;
        }

        public void setLogoutTime(Date logoutTime) {
            this.logoutTime = logoutTime;
        }

        public Queue<ChatMess> getMsgQueue() {
            return msgQueue;
        }

        public void setMsgQueue(Queue<ChatMess> msgQueue) {
            this.msgQueue = msgQueue;
        }

        public String getStorePath() {
            return storePath;
        }

        public void setStorePath(String storePath) {
            this.storePath = storePath;
        }

        /**
         * @return the sFileSocket
         */
        public SecSocket<ChatMess> getsFileSocket() {
            return sFileSocket;
        }

        /**
         * @param sFileSocket the sFileSocket to set
         */
        public void setsFileSocket(SecSocket<ChatMess> sFileSocket) {
            this.sFileSocket = sFileSocket;
        }

    }

    /**
     * 
     * @ClassName:  ServerSend   
     * @Description:读取用户的接收消息队列，并发送给用户   
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:55:27
     */
    private class ServerSend implements Runnable {

        private SecSocket<ChatMess> sSocket;
        private Client client;

        public ServerSend(SecSocket<ChatMess> sSocket, Client client) {
            this.sSocket = sSocket;
            this.client = client;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (client.getMsgQueue()) {
                    while (client.getMsgQueue().isEmpty() || sSocket.isClosed()) {
                        try {
                            client.getMsgQueue().wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    ChatMess chatMess = (ChatMess)client.getMsgQueue().peek();
                    boolean flag = sSocket.writeMsg(chatMess);
                    if (flag) {
                        client.getMsgQueue().remove();
                    }
                    client.getMsgQueue().notifyAll();
                }

            }

        }

    }

    /**
     * 
     * @ClassName:  ServerReceive   
     * @Description:接收客户端发送的消息，并存入目的用户的消息队列  
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:56:40
     */
    private class ServerReceive implements Runnable {

        private SecSocket<ChatMess> sSocket;
        private Client client;

        public ServerReceive(SecSocket<ChatMess> sSocket, Client client) {
            this.sSocket = sSocket;
            this.client = client;
        }

        @Override
        public void run() {

            while (!sSocket.isClosed()) {

                ChatMess chatMess = (ChatMess)sSocket.readMsg();
                if (chatMess == null) {
                    continue;
                }
                String cmd = chatMess.getCmd();
                String orderStr = chatMess.getMsg().replaceAll("\t|\r|\n", " ").trim();
                System.out.println(orderStr);
                switch (orderStr) {
                    case "help":
                        ChatMess helpMsg = new ChatMess("txt", showHelp());
                        helpMsg.setFrom("Server");
                        helpMsg.setFromIP("ServerIP");
                        helpMsg.setTime(new Date());
                        helpMsg.setToName(client.getName());
                        sSocket.writeMsg(helpMsg);
                        break;
                    case "list":
                        ChatMess listMsg = new ChatMess("txt", showAllUser());
                        listMsg.setFrom("Server");
                        listMsg.setFromIP("ServerIP");
                        listMsg.setTime(new Date());
                        listMsg.setToName(client.getName());
                        sSocket.writeMsg(listMsg);
                        break;
                    default:
                        int separatorIndex = orderStr.indexOf(":");
                        String toName;
                        String msg;
                        String from = client.getName();
                        String fromIP = sSocket.getIP();
                        if (separatorIndex > 3) {
                            toName = orderStr.substring(1, separatorIndex);
                        } else {
                            break;
                        }
                        if (orderStr.startsWith("@")) {
                            // Send message
                            msg = orderStr.substring(separatorIndex + 1);
                            if (msg != null) {
                                sendMsg(from, fromIP, toName, msg, cmd);
                            }

                        }
                        break;
                }
            }
            sSocket.close();

        }

    }

    /**
     * 
     * @ClassName:  RefreshClientList   
     * @Description:更新用户在线状态  
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:57:56
     */
    public class RefreshClientList implements Runnable {

        private Map<String, Client> clientList;

        public RefreshClientList(Map<String, Client> clientList) {
            // TODO Auto-generated constructor stub
            this.clientList = clientList;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub

            while (true) {
                try {
                    Thread.sleep(refreshInterval * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // System.out.println("Refresh User List");
                if (clientList.size() == 0) {
                    continue;
                }
                Iterator iterator = clientList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Client> entry = (Entry<String, Client>)iterator.next();
                    Client client = entry.getValue();
                    int state = client.getState();
                    if (client.sSocket.isClosed()) {
                        if (state != 0) {
                            client.setState(0);
                            client.setLogoutTime(new Date());
                        }
                    } else {
                        if (state == 0) {
                            client.setState(1);
                            client.setLoginTime(new Date());
                        }
                    }
                }
            }
        }

    }

    /**
     * 
     * @Title: sendMsg   
     * @Description: 将消息存入到用户的接收消息队列
     * @param from 消息来源
     * @param fromIP 来源IP
     * @param toName 消息目的用户
     * @param msg  消息内容
     * @param cmd  消息类型     
     * @return: void  
    * @date:   2019年4月4日 下午11:16:59     
     * @throws
     */
    public void sendMsg(String from, String fromIP, String toName, String msg, String cmd) {
        // TODO Auto-generated method stub
        if (toName.contentEquals("ALL")) {
            Iterator<Entry<String, Client>> iterator = clientList.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Client> entry = (Entry<String, Client>)iterator.next();
                Client client = entry.getValue();
                ChatMess chatMess = new ChatMess(cmd, msg);
                chatMess.setFrom(from);
                chatMess.setFromIP(fromIP);
                chatMess.setTime(new Date());
                chatMess.setToName(client.getName());
                new Thread() {
                    @Override
                    public void run() {
                        synchronized (client.getMsgQueue()) {
                            client.getMsgQueue().add(chatMess);
                            client.getMsgQueue().notifyAll();
                        }
                    }
                }.start();
            }
        } else {
            Set<String> keySet = clientList.keySet();
            if (keySet.contains(toName)) {
                Client client = clientList.get(toName);
                ChatMess chatMess = new ChatMess(cmd, msg);
                chatMess.setFrom(from);
                chatMess.setFromIP(fromIP);
                chatMess.setTime(new Date());
                new Thread() {

                    @Override
                    public void run() {
                        synchronized (client.getMsgQueue()) {
                            client.getMsgQueue().add(chatMess);
                            client.getMsgQueue().notifyAll();
                        }
                    }

                }.start();
            }
        }
    }

    /**
     * 
     * @Title: showHelp   
     * @Description:   
     * @param: @return   
     * @return: String  帮助信息字符串   
    * @date:   2019年4月4日 下午10:58:30     
     * @throws
     */

    /**
     * 
     * @Title: showHelp   
     * @Description: 帮助信息 
     * @return     帮助信息流
    * @date:   2019年4月4日 下午11:22:10     
     * @throws
     */
    private String showHelp() {

        StringBuilder sb = new StringBuilder();
        sb.append("===========Help=============\n");
        sb.append("COMMAND             DESC\n");
        sb.append("help                --show help" + "\n");
        sb.append("list                --show all online user" + "\n");
        sb.append("@user:msg           --send msg to user" + "\n");
        sb.append("@ALL:msg            --send msg to all user" + "\n");
        sb.append("#user:filepath      --send file to user" + "\n");
        sb.append("#ALL:filepath       --send file to all user" + "\n");
        sb.append("exit                --exit" + "\n");
        sb.append("============================" + "\n");

        return sb.toString();
    }

    /**
     * 
     * @Title: showAllUser   
     * @Description: 显示所有登录成功的用户列表   
     * @param: @return      
     * @return: String  用户列表字符串流
    * @date:   2019年4月4日 下午10:58:35     
     * @throws
     */

    /**
     * 
     * @Title: showAllUser   
     * @Description: 显示所有登录成功的用户列表 
     * @return    String  用户列表字符串流  
    * @date:   2019年4月4日 下午11:23:18     
     * @throws
     */
    private String showAllUser() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();

        sb.append("===========User List=============" + "\n");
        sb.append("UserName     State       IP          loginTime                   logoutTime\n");
        Iterator iterator = clientList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Client> entry = (Entry<String, Client>)iterator.next();
            Client client = entry.getValue();
            String state = client.getState() > 0 ? "on-line" : "off-line";
            String loginTime = sdf.format(client.getLoginTime());
            String logoutTime = client.getLogoutTime() != null ? sdf.format(client.getLogoutTime()) : "---";
            sb.append(client.getName() + "      " + state + "      " + client.getsSocket().getIP() + "      "
                + loginTime + "          " + logoutTime + "\n");
        }
        sb.append("=================================" + "\n");
        return sb.toString();

    }

    /**
     * 
     * @Title: sendLoginTips   
     * @Description: 发送登录提示  
     * @param secSocket
     * @param tips      
    * @date:   2019年4月4日 下午11:24:00     
     * @throws
     */
    private void sendLoginTips(SecSocket<ChatMess> secSocket, String tips) {
        StringBuffer msg = new StringBuffer();
        if (tips != null) {
            // msg.append("=================================\n"+"Tips:\n" + tips +
            // "\n=================================\n");
            msg.append("===============\n" + "Tips:\n" + tips + "\n===============\n");
        }

        msg.append("Hello,please input username and password.\n" + "(eg: JackMa=123456):\n");

        ChatMess chatMess = new ChatMess("txt", msg.toString());
        chatMess.setFrom("Server");
        chatMess.setFromIP("ServerIP");
        chatMess.setTime(new Date());
        secSocket.writeMsg(chatMess);
    }

    /**
     * 
     * @Title: loginSuccess   
     * @Description: 登录成功后操作 
     * @param name
     * @param shareKey
     * @param sSocket
     * @return      
    * @date:   2019年4月4日 下午11:24:24     
     * @throws
     */
    private Client loginSuccess(String name, String shareKey, SecSocket<ChatMess> sSocket) {
        Client client = new Client(name, shareKey, sSocket);
        client.setState(1);
        client.setLoginTime(new Date());
        // 判断是否登录过
        if (clientList.keySet().contains(name)) {
            Client oldClient = clientList.get(name);
            // 获取老的消息队列
            client.setMsgQueue(oldClient.getMsgQueue());
            if (oldClient.getState() > 0) {
                // 将老的登录用户挤下线
                ChatMess logoutMsg = new ChatMess("txt", "你已经被挤下线");
                logoutMsg.setFrom("Server");
                logoutMsg.setFromIP("Server");
                logoutMsg.setTime(new Date());
                oldClient.getsSocket().writeMsg(logoutMsg);
                oldClient.getsSocket().close();
            }
        }
        clientList.put(name, client);
        ChatMess chatMess = new ChatMess("txt", "login success!\n");
        chatMess.setFrom("Server");
        chatMess.setFromIP("ServerIP");
        chatMess.setTime(new Date());
        chatMess.setToName(name);
        sSocket.writeMsg(chatMess);
        return client;
    }

    /**
     * 
     * @Title: initSecureSocket   
     * @Description: 初始化并建立通信安全套接字 
     * @param socket  原始套接字
     * @return     String  返回安全套接字创建成功后用来加解密的shareKey 
    * @date:   2019年4月4日 下午11:24:48     
     * @throws
     */
    private String initSecureSocket(Socket socket) {
        // set Read TimeOut
        try {
            socket.setSoTimeout(60000);

        } catch (SocketException e1) {
            e1.printStackTrace();
        }

        ObjectInputStream readMsg = null;
        ObjectOutputStream writeMsg = null;
        try {

            int i = 0;
            while (i < 3) {

                i++;
                Thread.sleep(100);

                // Receive 1
                readMsg = new ObjectInputStream(socket.getInputStream());
                ExchangeMess receiveMsg1 = (ExchangeMess)readMsg.readObject();
                String cmd1 = receiveMsg1.getCmd();
                String publicKey = receiveMsg1.getPublicKey();
                if (!cmd1.contentEquals("syn-publicKey")) {
                    continue;
                }

                // Send 1
                writeMsg = new ObjectOutputStream(socket.getOutputStream());
                String shareKey = (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replace("-", "");
                byte[] enTestMsg = RSAUtils.encryptByPublicKey(shareKey.getBytes(), publicKey);
                ExchangeMess sendMsg1 = new ExchangeMess("ack-publicKey", serverPublicKey);
                sendMsg1.setTestMsg(enTestMsg);
                writeMsg.writeObject(sendMsg1);

                // Receive 2
                ExchangeMess receiveMsg2 = (ExchangeMess)readMsg.readObject();
                String cmd2 = receiveMsg2.getCmd();
                if (!cmd2.contentEquals("ack-testMsg")) {
                    continue;
                }

                byte[] deTestMsg = RSAUtils.decryptByPrivateKey(receiveMsg2.getTestMsg(), serverPrivateKey);
                String deTestMsgString = new String(deTestMsg);
                if (deTestMsgString.contentEquals(shareKey)) {

                    // Success Send 2
                    ExchangeMess sendMsg2 = new ExchangeMess("ack-testMsg-ok", "null");
                    writeMsg.writeObject(sendMsg2);

                    socket.setSoTimeout(0);
                    return shareKey;
                } else {
                    // Failed Send 2
                    ExchangeMess sendMsg2 = new ExchangeMess("ack-testMsg-err", "null");
                    writeMsg.writeObject(sendMsg2);
                }
            }

        } catch (IOException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            // 对象消息解析错误
            // e.printStackTrace();
            System.out.println("exchange publicKey failed!");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 
     * @Title: initFileSecureSocket   
     * @Description: 初始化并建立通信安全套接字 
     * @param socket 原始套接字
     * @return   Map<String, Object>   返回传输类型和客户端信息
    * @date:   2019年4月4日 下午11:25:20     
     * @throws
     */
    private Map<String, Object> initFileSecureSocket(Socket socket) {

        try {
            socket.setSoTimeout(60000);

        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        int i = 0;
        while (i < 3) {
            try {
                i++;
                // read syn-sendfile
                InputStream in = socket.getInputStream();
                ObjectInputStream objectIn = new ObjectInputStream(in);
                ExchangeMess exMsg = (ExchangeMess)objectIn.readObject();
                String cmd = exMsg.getCmd();
                String name = exMsg.getName();
                // byte[] testMsg = exMsg.getTestMsg();
                Client client = clientList.get(name);
                if (client != null && cmd.contentEquals("syn-sendfile")) {

                    // send ack-sendfile by secSocket
                    String shareKey = client.getShareKey();
                    String content = UUID.randomUUID().toString() + UUID.randomUUID().toString();
                    ExchangeMess ackMsg = new ExchangeMess("ack-sendfile", content.getBytes());
                    SecSocket<ExchangeMess> secSocket =
                        new SecSocket<ExchangeMess>(shareKey, socket, ExchangeMess.class);
                    secSocket.writeMsg(ackMsg);

                    // read syn-sendfile-2 from client by secSocket
                    ExchangeMess readMsg = secSocket.readMsg();
                    if (readMsg != null && readMsg.getCmd().contentEquals("syn-sendfile-2")) {
                        String checkContent = new String(readMsg.getTestMsg());
                        if (checkContent.contentEquals(content)) {
                            ackMsg.setCmd("ack-sendfile-ok");
                            secSocket.writeMsg(ackMsg);
                            socket.setSoTimeout(0);
                            Map res = new HashMap<String, Client>(maxUsers);
                            res.put("cmd", cmd);
                            res.put("client", client);
                            return res;
                        }
                    }
                } else if (client != null && cmd.contentEquals("syn-receivefile")) {
                    // send ack-sendfile by secSocket
                    String shareKey = client.getShareKey();
                    String content = UUID.randomUUID().toString() + UUID.randomUUID().toString();
                    ExchangeMess ackMsg = new ExchangeMess("ack-sendfile", content.getBytes());
                    SecSocket<ExchangeMess> secSocket =
                        new SecSocket<ExchangeMess>(shareKey, socket, ExchangeMess.class);
                    secSocket.writeMsg(ackMsg);

                    // read syn-sendfile-2 from client by secSocket
                    ExchangeMess readMsg = secSocket.readMsg();
                    if (readMsg != null && readMsg.getCmd().contentEquals("syn-sendfile-2")) {
                        String checkContent = new String(readMsg.getTestMsg());
                        if (checkContent.contentEquals(content)) {
                            ackMsg.setCmd("ack-sendfile-ok");
                            secSocket.writeMsg(ackMsg);
                            socket.setSoTimeout(0);
                            Map res = new HashMap<String, Client>(maxUsers);
                            res.put("cmd", cmd);
                            res.put("client", client);
                            return res;
                        }
                    }
                }

            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 
     * @Title: clientDispatcher   
     * @Description: 客户端消息连接请求处理器 
     * @param socket      
     * @return: void
     * @date:   2019年4月4日 下午11:30:12     
     * @throws
     */
    private void clientDispatcher(Socket socket) {

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, maxThreads, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.DiscardOldestPolicy());
        threadPool.execute(() -> {
            try {
                String shareKey = initSecureSocket(socket);
                if (shareKey != null) {
                    System.out.println("Server  Open SecureSocket");
                } else {
                    System.out.println("Server Do Not Open SecureSocket");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
                SecSocket<ChatMess> sSocket = new SecSocket<ChatMess>(shareKey, socket, ChatMess.class);
                sendLoginTips(sSocket, null);

                ChatMess chatMess;
                int i = 0;
                while (i < 3 && ((chatMess = (ChatMess)sSocket.readMsg()) != null)) {

                    i++;
                    String msg = chatMess.getMsg();
                    msg = msg.replaceAll("\t|\r|\n", " ");

                    // check whether or not to include "="
                    if (!msg.contains("=")) {
                        String tips = "the user name and password are not in " + " the correct format";
                        sendLoginTips(sSocket, tips);
                        continue;
                    }

                    // check the length and type
                    String[] userPwd = msg.split("=", 2);
                    userPwd[0] = userPwd[0].trim();
                    userPwd[1] = userPwd[1].trim();

                    System.out.println("UserName:" + userPwd[0]);
                    System.out.println("UserPasswd:" + userPwd[1]);

                    String regex = "^[a-zA-Z0-9]{5,15}$";
                    if (!Pattern.matches(regex, userPwd[0]) || !Pattern.matches(regex, userPwd[1])) {

                        String tips =
                            "UserName or PassWord must is letter or number \n" + "and the length must between 5 and 15";
                        sendLoginTips(sSocket, tips);
                        continue;
                    }

                    // check password
                    if (userPwd[1].contentEquals(defaultPwd)) {

                        Client client = loginSuccess(userPwd[0], shareKey, sSocket);
                        Thread serverSender = new Thread(new ServerSend(sSocket, client));
                        Thread serverReceiver = new Thread(new ServerReceive(sSocket, client));
                        serverSender.start();
                        serverReceiver.start();
                        return;
                    } else {
                        sendLoginTips(sSocket, null);
                        continue;
                    }
                }
                // 连续输入三次密码错误，关闭套接字
                sSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                threadPool.shutdown();// 关闭线程池
            }
        });
    }

    /**
     * 
     * @Title: fileDispatcher   
     * @Description: 文件传输请求处理器 
     * @param socket    原始套接字  
     * @return: void
     * @date:   2019年4月4日 下午11:31:33     
     * @throws
     */
    public void fileDispatcher(Socket socket) {

        new Thread() {
            private DataInputStream dis;
            private DataOutputStream dos;
            private FileOutputStream fos;
            private FileInputStream fis;

            @Override
            public void run() {
                try {
                    Map<String, Object> resMap = initFileSecureSocket(socket);

                    if (resMap != null) {
                        System.out.println("Server  Open File SecureSocket");
                    } else {
                        System.out.println("Server Do Not Open File SecureSocket");
                        return;
                    }

                    String cmd = (String)resMap.get("cmd");
                    Client client = (Client)resMap.get("client");
                    if (cmd.contentEquals("syn-sendfile")) {
                        // 客户端发送文件，服务器接收文件
                        dis = new DataInputStream(socket.getInputStream());

                        String toName = dis.readUTF();
                        String fileName = dis.readUTF();
                        // long fileLength = dis.readLong();
                        File dir = new File(baseStorePath + File.separatorChar + toName);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        File file = new File(dir.getAbsolutePath() + File.separatorChar + fileName);
                        fos = new FileOutputStream(file);
                        while (true) {

                            long len = 0;
                            try {
                                len = dis.readLong();
                            } catch (Exception e) {
                                // TODO: handle exception
                                // e.printStackTrace();
                                // System.out.println("len: " + len);
                                break;
                            }
                            // System.out.println("send len: " + len);

                            if (len > 0) {

                                // System.out.println("available: " + dis.available());
                                byte[] data = new byte[(int)len];
                                int readNum = 0;
                                // 由于网络原因，一次read很可能没有读取所有内容，
                                // 如果不加这个while循环容易导致解密失败。
                                while (readNum < len) {
                                    readNum += dis.read(data, readNum, (int)(len - readNum));
                                }

                                String dataString = new String(data, "utf-8");

                                // System.out.println("data len: " + data.length);
                                // System.out.println("shareKey: " + client.getShareKey());
                                // System.out.println("dataString: " + dataString);
                                String deString = AESUtils.decrypt(dataString, client.getShareKey());
                                // System.out.println("Base64Data: " + deString);
                                byte[] deData = Base64Utils.decode(deString);

                                fos.write(deData, 0, deData.length);
                                fos.flush();
                            } else {
                                break;
                            }
                        }
                    } else if (cmd.contentEquals("syn-receivefile")) {
                        // 服务器发送文件，客户端接收文件
                        dis = new DataInputStream(socket.getInputStream());

                        String fileName = dis.readUTF();
                        File file = new File(
                            baseStorePath + File.separatorChar + client.getName() + File.separatorChar + fileName);
                        String shareKey = client.getShareKey();
                        if (file.exists()) {

                            dos = new DataOutputStream(socket.getOutputStream());
                            System.out.println("========开始传输文件========");
                            byte[] bytes = new byte[1024];
                            int length = 0;
                            long progress = 0;
                            fis = new FileInputStream(file);
                            dos.writeLong(file.length());
                            dos.flush();
                            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {

                                String base64Data = Base64Utils.encode(Arrays.copyOfRange(bytes, 0, length));
                                String enData = AESUtils.encrypt(base64Data, shareKey);
                                byte[] enBytes = enData.getBytes("utf-8");

                                dos.writeLong(enBytes.length);
                                dos.flush();
                                dos.write(enBytes, 0, enBytes.length);
                                dos.flush();

                                progress += length;
                                dos.writeLong(progress);
                                dos.flush();
                                System.out.print("=>" + (100 * progress / file.length()) + "%");
                            }
                            System.out.println();
                            System.out.println("========文件传输成功========");
                            dos.close();
                            fis.close();
                            file.delete();

                        } else {
                            System.out.println("File is not existed: " + file.getAbsolutePath());
                        }

                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (dos != null) {
                            dos.close();
                        }
                        if (fis != null) {
                            fis.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                        if (dis != null) {
                            dis.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }.start();

    }

    /**
     * 
     * @Title: start   
     * @Description: 启动服务端   
     * @param:       
     * @return: void  
    * @date:   2019年4月4日 下午10:59:09     
     * @throws
     */
    public void start() {

        // System.err.println("env:" + env.getProperty("tcpServer.defaultPwd") );

        // refresh clientList
        Thread refreshClientList = new Thread(new RefreshClientList(clientList));
        refreshClientList.start();

        // start msg Server
        Thread clientDispacher = new Thread() {

            @Override
            public void run() {

                ServerSocket serverSock = null;
                try {
                    serverSock = new ServerSocket(serverPort);
                    while (true) {
                        Socket socket = serverSock.accept();
                        clientDispatcher(socket);
                    }

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    if (serverSock != null) {
                        try {
                            serverSock.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }

        };

        // start file Server
        Thread fileDispacher = new Thread() {
            @Override
            public void run() {
                ServerSocket serverFileSock;
                try {
                    serverFileSock = new ServerSocket(serverFilePort);
                    while (true) {
                        Socket socket = serverFileSock.accept();
                        fileDispatcher(socket);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }

            }
        };

        // start console manage
        Thread consoleDispatcher = new Thread() {
            @Override
            public void run() {
                Scanner in = new Scanner(System.in);
                while (true) {
                    String cmd = in.nextLine().trim();
                    switch (cmd) {
                        case "list":
                            System.out.println(showAllUser());;
                            break;
                        case "exit":
                            System.out.println("server is exiting...");
                            System.exit(0);
                        case "quit":
                            System.out.println("server is exiting...");
                            System.exit(0);
                        default:
                            break;
                    }
                }
            }
        };

        clientDispacher.start();
        fileDispacher.start();
        consoleDispatcher.start();

        try {
            clientDispacher.join();
            fileDispacher.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("server exit...");
    }
}

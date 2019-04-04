package com.godo.playChat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

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
 * @ClassName:  TcpClient   
 * @Description:创建客户端对象   
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午9:20:36
 */
@Service("tcpClient")
public class TcpClient {
    /** 服务端IP **/
    @Value("${tcpServer.serverIP}")
    private String serverIP = "127.0.0.1";
    /** 服务端通信端口 **/
    @Value("${tcpServer.serverPort}")
    private int serverPORT = 3434;
    /** 服务端文件传输端口 **/
    @Value("${tcpServer.filePort}")
    private int serverFilePORT = 3535;
    /** 客户端接收文件的目录 **/
    @Value("${tcpClient.baseStorePath}")
    private String baseStore = "./clientStoreFile";
    /** 客户端生成的密钥对 **/
    private final Map<String, Object> keyPair = RSAUtils.genKeyPair();
    /** 客户端公钥 **/
    private final String clientPublicKey = RSAUtils.getPublicKey(keyPair);
    /** 客户端私钥 **/
    private final String clientPrivateKey = RSAUtils.getPrivateKey(keyPair);
    /** 协商出来的用于加密数据的密钥字符串 **/
    private String shareKey = null;
    /** 客户端的登录用户名 **/
    private String myName;

    /**
     * 
     * @ClassName:  SendFile   
     * @Description:发送文件
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:37:05
     */
    private class SendFile implements Runnable {

        /** 需要传输的文件路径 **/
        private String filePath;
        /** 传输对象的用户名 **/
        private String toName;
        /** 传输用的套接字 **/
        private SecSocket<ChatMess> secSocket;
        private FileInputStream fis;
        private DataOutputStream dos;

        /**
         * 
         * @Title:  SendFile   
         * @Description:初始化发送文件类
         * @param:  @param toName   发送给哪个用户
         * @param:  @param filePath 文件路径
         * @param:  @param secSocket  使用的套接字
         * @throws
         */
        public SendFile(String toName, String filePath, SecSocket<ChatMess> secSocket) {
            this.toName = toName;
            this.filePath = filePath;
            this.secSocket = secSocket;
        }

        @Override
        public void run() {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    fis = new FileInputStream(file);
                    dos = new DataOutputStream(secSocket.getSocket().getOutputStream());
                    dos.writeUTF(toName);
                    dos.flush();
                    dos.writeUTF(file.getName());
                    dos.flush();

                    Date sendBegin = new Date();
                    System.out.println("========开始上传离线文件========");
                    byte[] bytes = new byte[1024];
                    int length = 0;
                    long progress = 0;
                    while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                        // 将实际读取到的字节数进行编码
                        String base64Data = Base64Utils.encode(Arrays.copyOfRange(bytes, 0, length));
                        String enStr = AESUtils.encrypt(base64Data, shareKey);

                        byte[] enBytes = enStr.getBytes("utf-8");
                        dos.writeLong(enBytes.length);
                        dos.flush();

                        dos.write(enBytes, 0, enBytes.length);
                        dos.flush();
                        progress += length;
                        System.out.print("=>" + (100 * progress / file.length()) + "%");
                    }
                    System.out.println();
                    System.out.println("========上传离线文件成功========");
                    Date sendEnd = new Date();
                    long timeConsume = (sendEnd.getTime() - sendBegin.getTime()) / 1000;
                    System.out.println("文件大小: " + file.length() / 1000 + "(kb)");
                    System.out.println("传输用时: " + timeConsume + "s");
                } else {
                    System.out.println("File is not existed: " + filePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dos != null) {
                        dos.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                secSocket.close();
            }

        }

    }

    /**
     * 
     * @ClassName:  ReceiveFile   
     * @Description:接收文件
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:40:45
     */
    private class ReceiveFile implements Runnable {

        private String fileName;
        private SecSocket<ChatMess> secSocket;
        private FileOutputStream fos;
        private DataOutputStream dos;
        private DataInputStream dis;

        public ReceiveFile(String fileName, SecSocket<ChatMess> secSocket) {
            this.fileName = fileName;
            this.secSocket = secSocket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(secSocket.getSocket().getInputStream());
                dos = new DataOutputStream(secSocket.getSocket().getOutputStream());
                dos.writeUTF(fileName);
                dos.flush();
                File dir = new File(baseStore + File.separatorChar + myName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir.getAbsolutePath() + File.separatorChar + fileName);
                fos = new FileOutputStream(file);
                long fileLength = dis.readLong();

                Date sendBegin = new Date();
                System.out.println("========开始接收离线文件========");
                while (true) {

                    long len = 0;
                    try {
                        len = dis.readLong();
                    } catch (Exception e) {
                        break;
                    }

                    if (len > 0) {
                        byte[] data = new byte[(int)len];
                        int readNum = 0;
                        // 由于网络原因，一次read很可能没有读取所有内容，
                        // 如果不加这个while循环容易导致解密失败。
                        while (readNum < len) {
                            readNum += dis.read(data, readNum, (int)(len - readNum));
                        }

                        String dataString = new String(data, "utf-8");
                        String deString = AESUtils.decrypt(dataString, shareKey);
                        byte[] deData = Base64Utils.decode(deString);
                        fos.write(deData, 0, deData.length);
                        fos.flush();
                    } else {
                        break;
                    }
                    long progress = dis.readLong();
                    System.out.print("=>" + (100 * progress / fileLength) + "%");
                }
                System.out.println();
                System.out.println("========接收离线文件完成========");
                Date sendEnd = new Date();
                long timeConsume = (sendEnd.getTime() - sendBegin.getTime()) / 1000;
                System.out.println("文件大小: " + fileLength / 1000 + "(kb)");
                System.out.println("传输用时: " + timeConsume + "(s)");

                System.out.println("Receive File : " + file.getAbsolutePath());
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (dis != null) {
                        dis.close();
                    }
                    if (dos != null) {
                        dos.close();
                    }
                    secSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 
     * @ClassName:  SocketReceive   
     * @Description: 接收来自套接字的消息   
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:41:39
     */
    private class SocketReceive implements Runnable {

        private SecSocket<ChatMess> sSocket;

        public SocketReceive(SecSocket<ChatMess> socket) {
            this.sSocket = socket;
        }

        @Override
        public void run() {
            while (!sSocket.isClosed()) {

                ChatMess chatMess = sSocket.readMsg();
                if (chatMess == null) {
                    continue;
                }
                String cmd = chatMess.getCmd();
                String msg = chatMess.getMsg();
                String from = chatMess.getFrom();
                String fromIP = chatMess.getFromIP();
                String toName = chatMess.getToName();
                if (myName == null && toName != null) {
                    myName = toName;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date time = chatMess.getTime();
                String timeStamp = time != null ? sdf.format(time) : "---";

                if (cmd.contentEquals("file")) {

                    File filePath = new File(baseStore + File.separatorChar + msg.trim());

                    Socket fileSocket = null;
                    try {
                        fileSocket = new Socket(serverIP, serverFilePORT);
                        SecSocket<ExchangeMess> exSecSocket =
                            new SecSocket<ExchangeMess>(shareKey, fileSocket, ExchangeMess.class);
                        boolean flag = initFileSecureSocket(exSecSocket, "syn-receivefile");
                        if (flag) {
                            System.out.println("initFileSecureSocket success");
                            SecSocket<ChatMess> fileSecSocket =
                                new SecSocket<ChatMess>(shareKey, fileSocket, ChatMess.class);
                            Thread receiver = new Thread(new ReceiveFile(filePath.getName(), fileSecSocket));
                            receiver.start();
                            receiver.join();
                            // send msg
                            chatMess = new ChatMess("file-ok", "@" + from + ":" + filePath.getName());
                            if (sSocket.isClosed()) {
                                System.out.println("Socket is Closed, Send Failed!");
                                break;
                            }
                            sSocket.writeMsg(chatMess);

                        } else {
                            System.out.println("initFileSecureSocket failed");
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        if (fileSocket != null) {
                            try {
                                fileSocket.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                    System.out.println("Receive File : " + msg + " from " + from + "(" + fromIP + ") at " + timeStamp);

                } else if (cmd.contentEquals("file-ok")) {
                    // 文件接收成功的消息
                    System.out.println(from + "(" + fromIP + ")" + timeStamp + " : \n" + "文件已接收：" + msg);
                } else {
                    // 聊天消息
                    System.out.println(from + "(" + fromIP + ")" + timeStamp + " : \n" + msg);

                }

            }
            sSocket.close();
            System.exit(1);

        }

    }

    /**
     * 
     * @ClassName:  SocketSend   
     * @Description:发送消息到套接字  
     * @author: DaLiangZao 
     * @date:   2019年4月4日 下午10:42:08
     */
    private class SocketSend implements Runnable {

        private SecSocket<ChatMess> sSocket;

        public SocketSend(SecSocket<ChatMess> socket) {
            this.sSocket = socket;
        }

        @Override
        public void run() {

            Scanner scanner = new Scanner(System.in);

            while (!sSocket.isClosed()) {

                String s = scanner.nextLine();
                s = s.replaceAll("\t|\r|\n", " ").trim();
                ChatMess chatMess = null;
                if (s.startsWith("#")) {
                    // send file
                    int index = s.indexOf(":");
                    String name = s.substring(1, index).trim();
                    String filePath = s.substring(index + 1).trim();
                    Socket fileSocket = null;
                    try {
                        fileSocket = new Socket(serverIP, serverFilePORT);
                        SecSocket<ExchangeMess> exSecSocket =
                            new SecSocket<ExchangeMess>(shareKey, fileSocket, ExchangeMess.class);
                        boolean flag = initFileSecureSocket(exSecSocket, "syn-sendfile");
                        if (flag) {
                            System.out.println("initFileSecureSocket success");
                            SecSocket<ChatMess> fileSecSocket =
                                new SecSocket<ChatMess>(shareKey, fileSocket, ChatMess.class);
                            Thread fileSender = new Thread(new SendFile(name, filePath, fileSecSocket));
                            fileSender.start();
                            fileSender.join();
                            // send msg
                            // System.out.println("send file msg");
                            File file = new File(filePath);
                            chatMess = new ChatMess("file", "@" + name + ":" + file.getName());

                        } else {
                            System.out.println("initFileSecureSocket failed, Do Not Send File!");
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        if (fileSocket != null) {
                            try {
                                fileSocket.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                    }

                } else if (s.startsWith("exit") || s.startsWith("quit")) {
                    System.out.println("client is exiting...");
                    try {
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.exit(0);

                } else {
                    // send msg
                    chatMess = new ChatMess("txt", s);
                }
                if (sSocket.isClosed()) {
                    System.out.println("Socket is Closed, Send Failed!");
                    break;
                }
                sSocket.writeMsg(chatMess);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            scanner.close();

        }

    }

    /**
     * 
     * @Title: initFileSecureSocket   
     * @Description:  初始化并建立文件传输的安全套接字  
     * @param: @param secSocket   套接字
     * @param: @param cmd  命令类型
     * @param: @return      
     * @return: boolean  是否初始化成功
    * @date:   2019年4月4日 下午10:42:33     
     * @throws
     */
    private boolean initFileSecureSocket(SecSocket<ExchangeMess> secSocket, String cmd) {
        // set Read TimeOut
        try {
            secSocket.getSocket().setSoTimeout(60000);

        } catch (SocketException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }

        ObjectOutputStream writeMsg = null;
        try {
            int i = 0;
            while (i < 3) {

                i++;
                Thread.sleep(100);
                // Send 1
                ExchangeMess sendMsg1 = new ExchangeMess(cmd, "null");
                // 告诉接收方，我的名字，以便后续使用shareKey加密
                sendMsg1.setName(myName);
                writeMsg = new ObjectOutputStream(secSocket.getSocket().getOutputStream());
                writeMsg.writeObject(sendMsg1);

                // Receive 1
                ExchangeMess receiveMsg1 = secSocket.readMsg();
                String cmd1 = receiveMsg1.getCmd();
                if (!cmd1.contentEquals("ack-sendfile")) {
                    continue;
                }

                // Send 2
                receiveMsg1.setCmd("syn-sendfile-2");
                secSocket.writeMsg(receiveMsg1);

                // Receive 2
                ExchangeMess receiveMsg2 = secSocket.readMsg();
                String cmd2 = receiveMsg2.getCmd();
                if (cmd2.contentEquals("ack-sendfile-ok")) {
                    // 恢复套接字超时的默认值
                    secSocket.getSocket().setSoTimeout(0);
                    return true;
                }

            }

        } catch (IOException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 
     * @Title: initSecureSocket   
     * @Description:  初始化并建立通信安全套接字
     * @param: @param socket  原始套接字
     * @param: @return      
     * @return: String  返回安全套接字创建成功后用来加解密的shareKey
    * @date:   2019年4月4日 下午10:46:31     
     * @throws
     */
    private String initSecureSocket(Socket socket) {
        // set Read TimeOut
        try {
            socket.setSoTimeout(60000);

        } catch (SocketException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        ObjectOutputStream writeMsg = null;
        ObjectInputStream readMsg = null;
        try {
            int i = 0;
            while (i < 3) {

                i++;
                Thread.sleep(100);
                // Send 1
                writeMsg = new ObjectOutputStream(socket.getOutputStream());
                ExchangeMess sendMsg1 = new ExchangeMess("syn-publicKey", clientPublicKey);
                writeMsg.writeObject(sendMsg1);

                // Receive 1
                readMsg = new ObjectInputStream(socket.getInputStream());
                ExchangeMess receiveMsg1 = (ExchangeMess)readMsg.readObject();
                String cmd1 = receiveMsg1.getCmd();
                if (!cmd1.contentEquals("ack-publicKey")) {
                    continue;
                }
                String publicKey = receiveMsg1.getPublicKey();
                byte[] testMsg = receiveMsg1.getTestMsg();
                String shareKey = new String(RSAUtils.decryptByPrivateKey(testMsg, clientPrivateKey));

                // Send 2
                ExchangeMess sendMsg2 = new ExchangeMess("ack-testMsg", "null");
                byte[] enTestMsg = RSAUtils.encryptByPublicKey(shareKey.getBytes(), publicKey);
                sendMsg2.setTestMsg(enTestMsg);
                writeMsg.writeObject(sendMsg2);

                // Receive 2
                ExchangeMess receiveMsg2 = (ExchangeMess)readMsg.readObject();
                String cmd2 = receiveMsg2.getCmd();
                if (cmd2.contentEquals("ack-testMsg-ok")) {
                    // 恢复套接字超时的默认值
                    socket.setSoTimeout(0);
                    return shareKey;
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

        return null;
    }

    /**
     * 
     * @Title: start   
     * @Description: 启动客户端  
     * @param:       
     * @return: void  
    * @date:   2019年4月4日 下午10:50:12     
     * @throws
     */
    public void start() {

        Socket socket = null;
        try {
            socket = new Socket(serverIP, serverPORT);
            shareKey = initSecureSocket(socket);
            if (shareKey != null) {
                System.out.println("Client Open SecureSocket");
            } else {
                System.out.println("Client Do Not Open SecureSocket");
            }

            SecSocket<ChatMess> sSocket = new SecSocket<ChatMess>(shareKey, socket, ChatMess.class);

            Thread tSend = new Thread(new SocketSend(sSocket));
            Thread tReceive = new Thread(new SocketReceive(sSocket));
            tSend.start();
            tReceive.start();
            System.out.println("## client start success ##");
            tSend.join();
            tReceive.join();

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println("Connection refused: (serverIP: " + serverIP + " || serverPORT: " + serverPORT
                + "|| serverFilePort:" + serverFilePORT + ")");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            System.out.println("client exit...");
        }
    }

}

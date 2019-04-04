package com.godo.playChat.common.utils.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godo.playChat.common.utils.AESUtils;

/**
 * 
 * @ClassName:  SecSocket   
 * @Description: 安全套接字封装  
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午11:40:27     
 * @param <T>
 */
public class SecSocket<T> {

    /** 公钥 **/
    private String shareKey;
    /** 原始套接字 **/
    private Socket socket;
    /** 泛型 **/
    private Class clazz;

    /**
     * 
     * @Title:  SecSocket   
     * @Description:  初始化安全套接字 
     * @param shareKey
     * @param socket
     * @param t  
     * @throws
     */
    public SecSocket(String shareKey, Socket socket, Class t) {
        this.shareKey = shareKey;
        this.socket = socket;

        this.clazz = t;
    }

    /**
     * 
     * @Title: writeMsg   
     * @Description: 发送消息 
     * @param chatMess  消息对象
     * @return: boolean  是否发送成功
     * @date:   2019年4月4日 下午11:41:40     
     * @throws
     */
    public boolean writeMsg(T chatMess) {

        if (chatMess == null) {
            return true;
        }
        try {

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(chatMess);
            String encrypt = AESUtils.encrypt(jsonString, shareKey);

            // System.err.println("base64Encrypt: " + encrypt + "\n shareKey: " +
            // shareKey );
            new ObjectOutputStream(socket.getOutputStream()).writeObject(encrypt);
            return true;

        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Socket Write is Closed!");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 
     * @Title: readMsg   
     * @Description: 读取消息对象 
     * @return: T  获得的消息对象
     * @date:   2019年4月4日 下午11:42:41     
     * @throws
     */
    public T readMsg() {

        ObjectMapper mapper = new ObjectMapper();
        try {
            String base64Encrypt = (String)new ObjectInputStream(socket.getInputStream()).readObject();
            // System.err.println("base64Encrypt: " + base64Encrypt + "\n shareKey: " +
            // shareKey );
            String jsonDecrypt = new String(AESUtils.decrypt(base64Encrypt, shareKey));

            T chatMess = (T)mapper.readValue(jsonDecrypt, clazz);

            return chatMess;

        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: handle exception
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            // e.printStackTrace();
            System.out.println("Socket Read is Closed!");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 
     * @Title: close   
     * @Description: 关闭套接字       
     * @return: void
     * @date:   2019年4月4日 下午11:43:13     
     * @throws
     */
    public void close() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @Title: isClosed   
     * @Description: 判断套接字是否关闭 
     * @return      
     * @return: boolean
     * @date:   2019年4月4日 下午11:43:25     
     * @throws
     */
    public boolean isClosed() {

        return socket.isClosed();
    }

    /**
     * 
     * @Title: getIP   
     * @Description: 获取套接字对端IP 
     * @return      
     * @return: String
     * @date:   2019年4月4日 下午11:43:41     
     * @throws
     */
    public String getIP() {

        return socket.getInetAddress().getHostAddress();
    }

    /**
     * @return the shareKey
     */
    public String getShareKey() {
        return shareKey;
    }

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @param shareKey the shareKey to set
     */
    public void setShareKey(String shareKey) {
        this.shareKey = shareKey;
    }

    /**
     * @param socket the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}

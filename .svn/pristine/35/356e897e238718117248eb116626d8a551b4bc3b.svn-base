package com.godo.playChat.common.utils.message;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @ClassName:  ChatMess   
 * @Description:消息体对象  
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午11:33:28
 */
public class ChatMess implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息类型 **/
    private String cmd;
    /** 消息的目的用户 **/
    private String toName;
    /** 消息内容 **/
    private String msg;
    /** 暂时没有使用 **/
    private byte[] file;
    /** 消息来源 **/
    private String from;
    /** 消息来源IP **/
    private String fromIP;
    /** 消息时间戳 **/
    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public ChatMess() {};

    public ChatMess(String cmd, String msg) {
        this.cmd = cmd;
        this.msg = msg;
    }

    public String getCmd() {
        return cmd;
    }

    public String getMsg() {
        return msg;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromIP() {
        return fromIP;
    }

    public void setFromIP(String fromIP) {
        this.fromIP = fromIP;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    /**
     * @return the toName
     */
    public String getToName() {
        return toName;
    }

    /**
     * @param toName the toName to set
     */
    public void setToName(String toName) {
        this.toName = toName;
    }

}

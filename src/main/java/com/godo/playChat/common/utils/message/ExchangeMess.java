package com.godo.playChat.common.utils.message;

import java.io.Serializable;

/**
 * 
 * @ClassName:  ExchangeMess   
 * @Description:密钥交换对象
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午11:33:53
 */
public class ExchangeMess implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息类型 **/
    private String cmd;
    /**  用户名 **/
    private String name;
    /** 公钥 **/
    private String publicKey;
    /** 测试数据 **/
    private byte[] testMsg;

    public ExchangeMess() {

    }

    public ExchangeMess(String cmd, String publicKey) {

        this.cmd = cmd;
        this.publicKey = publicKey;
    }

    public ExchangeMess(String cmd, byte[] testMsg) {

        this.cmd = cmd;
        this.testMsg = testMsg;
    }

    public String getCmd() {
        return cmd;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getTestMsg() {
        return testMsg;
    }

    public void setTestMsg(byte[] testMsg) {
        this.testMsg = testMsg;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}
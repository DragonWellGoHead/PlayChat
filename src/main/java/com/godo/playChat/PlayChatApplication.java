package com.godo.playChat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.godo.playChat.client.TcpClient;
import com.godo.playChat.server.TcpServer;

/**
 * 
 * @ClassName:  PlayChatApplication   
 * @Description: 启动主程序  
 * @author: DaLiangZao 
 * @date:   2019年4月4日 下午10:24:23
 */
@SpringBootApplication
public class PlayChatApplication implements CommandLineRunner {

    public static void main(String[] args) {

        SpringApplication.run(PlayChatApplication.class, args);
    }

    @Autowired
    private TcpServer tcpServer;

    @Autowired
    private TcpClient tcpClient;

    /**
     * 
     * @Title: showUsage   
     * @Description: 用法      
     * @return: void
     * @date:   2019年4月4日 下午11:56:25     
     * @throws
     */
    private static void showUsage() {

        System.out.println("Usage:");
        System.out.println("  java playChat -server");
        System.out.println("  java playChat -client");
    }

    @Override
    public void run(String... args) throws Exception {

        if (args.length <= 0) {
            showUsage();
            return;
        }

        if (args[0].contentEquals("-server")) {
            System.out.println("ChatServer Start...");
            tcpServer.start();
        } else if (args[0].contentEquals("-client")) {
            System.out.println("ChatClient Start...");
            tcpClient.start();
        } else {
            showUsage();
            return;
        }
    }

}

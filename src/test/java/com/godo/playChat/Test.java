package com.godo.playChat;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.godo.playChat.common.utils.AESUtils;


public class Test {
    
    private static class Client{
        private String data1;
        private String data2;
        
        public Client(String data1, String data2) {
            this.data1 = data1;
            this.data2 = data2;
        }
        
        public String getData1() {
            return data1;
        }
        public String getData2() {
            return data2;
        }
        public void setData1(String data1) {
            this.data1 = data1;
        }
        public void setData2(String data2) {
            this.data2 = data2;
        }
        @Override
        public String toString() {
            return "Client [data1=" + data1 + ", data2=" + data2 + "]";
        }
        
    }

    public static void  main(String[] args) {
        Map<String, Client> map = new ConcurrentHashMap<String, Client>();
        Client aa = new Client("aa-1", "aa-2");
        Client bb = new Client("bb-1", "bb-2");
        Client cc = new Client("cc-1", "cc-2");
        map.put("aa", aa);
        map.put("bb", bb);
        map.put("cc", cc);
        Iterator iterator = map.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, Client> entry = (Entry<String, Client>) iterator.next();
            String key = entry.getKey();
            if (key.contentEquals("bb")) {
                Client client = entry.getValue();
                client.setData2("xx-2");
            }
        }
        for(String key : map.keySet()) {
            
            System.out.println(map.get(key));
        }
        byte[] data = "thisisamsg".getBytes();
        System.out.println("明文长度：" + data.length);
        byte[] enData = AESUtils.encrypt(data, "asdfksdfjskgjdfoidfkgkdflgldflgkdfklg");
        System.out.println("密文长度：" + enData.length);
        
        
        
    }
}

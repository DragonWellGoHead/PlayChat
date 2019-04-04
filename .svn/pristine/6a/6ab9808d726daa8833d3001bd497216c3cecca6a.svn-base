package com.godo.playChat.common.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/** *//**
* <p>
* RSA公钥/私钥/签名工具包
* </p>
* <p>
* 罗纳德·李维斯特（Ron [R]ivest）、阿迪·萨莫尔（Adi [S]hamir）和伦纳德·阿德曼（Leonard [A]dleman）
* </p>
* <p>
* 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
* 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
* 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
* </p>
* 
* @author IceWee
* @date 2012-4-26
* @version 1.0
*/
public class RSAUtils {

   /** *//**
    * 加密算法RSA
    */
   public static final String KEY_ALGORITHM = "RSA";
   
   /** *//**
    * 签名算法
    */
   public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

   /** *//**
    * 获取公钥的key
    */
   private static final String PUBLIC_KEY = "RSAPublicKey";
   
   /** *//**
    * 获取私钥的key
    */
   private static final String PRIVATE_KEY = "RSAPrivateKey";
   
   /** *//**
    * RSA最大加密明文大小
    */
   private static final int MAX_ENCRYPT_BLOCK = 117;
   
   /** *//**
    * RSA最大解密密文大小
    */
   private static final int MAX_DECRYPT_BLOCK = 128;


   /** *//**
    * <p>
    * 生成密钥对(公钥和私钥)
    * </p>
    * 
    * @return
    * @throws Exception
    */
   public static Map<String, Object> genKeyPair()  {
       try {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
           keyPairGen.initialize(1024);
           KeyPair keyPair = keyPairGen.generateKeyPair();
           RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
           RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
           Map<String, Object> keyMap = new HashMap<String, Object>(2);
           keyMap.put(PUBLIC_KEY, publicKey);
           keyMap.put(PRIVATE_KEY, privateKey);
           return keyMap;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
   }
   
   /** *//**
    * <p>
    * 用私钥对信息生成数字签名
    * </p>
    * 
    * @param data 已加密数据
    * @param privateKey 私钥(BASE64编码)
    * 
    * @return
    * @throws Exception
    */
   public static String sign(byte[] data, String privateKey)   {
       try {
        byte[] keyBytes = Base64Utils.decode(privateKey);
           PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
           KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
           PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
           Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
           signature.initSign(privateK);
           signature.update(data);
           return Base64Utils.encode(signature.sign());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return null;
   }

   /** *//**
    * <p>
    * 校验数字签名
    * </p>
    * 
    * @param data 已加密数据
    * @param publicKey 公钥(BASE64编码)
    * @param sign 数字签名
    * 
    * @return
    * @throws Exception
    * 
    */
   public static boolean verify(byte[] data, String publicKey, String sign)
           throws Exception {
       byte[] keyBytes = Base64Utils.decode(publicKey);
       X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
       KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
       PublicKey publicK = keyFactory.generatePublic(keySpec);
       Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
       signature.initVerify(publicK);
       signature.update(data);
       return signature.verify(Base64Utils.decode(sign));
   }

   /** *//**
    * <P>
    * 私钥解密
    * </p>
    * 
    * @param encryptedData 已加密数据
    * @param privateKey 私钥(BASE64编码)
    * @return
    * @throws Exception
    */
   public static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey)
   {
       try {
        byte[] keyBytes = Base64Utils.decode(privateKey);
           PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
           KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
           Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
           Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
           cipher.init(Cipher.DECRYPT_MODE, privateK);
           int inputLen = encryptedData.length;
           ByteArrayOutputStream out = new ByteArrayOutputStream();
           int offSet = 0;
           byte[] cache;
           int i = 0;
           // 对数据分段解密
           while (inputLen - offSet > 0) {
               if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                   cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
               } else {
                   cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
               }
               out.write(cache, 0, cache.length);
               i++;
               offSet = i * MAX_DECRYPT_BLOCK;
           }
           byte[] decryptedData = out.toByteArray();
           out.close();
           return decryptedData;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return null;
   }

   /** *//**
    * <p>
    * 公钥解密
    * </p>
    * 
    * @param encryptedData 已加密数据
    * @param publicKey 公钥(BASE64编码)
    * @return
    * @throws Exception
    */
   public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey)
   {
       try {
        byte[] keyBytes = Base64Utils.decode(publicKey);
           X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
           KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
           Key publicK = keyFactory.generatePublic(x509KeySpec);
           Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
           cipher.init(Cipher.DECRYPT_MODE, publicK);
           int inputLen = encryptedData.length;
           ByteArrayOutputStream out = new ByteArrayOutputStream();
           int offSet = 0;
           byte[] cache;
           int i = 0;
           // 对数据分段解密
           while (inputLen - offSet > 0) {
               if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                   cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
               } else {
                   cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
               }
               out.write(cache, 0, cache.length);
               i++;
               offSet = i * MAX_DECRYPT_BLOCK;
           }
           byte[] decryptedData = out.toByteArray();
           out.close();
           return decryptedData;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return null;
   }

   /** *//**
    * <p>
    * 公钥加密
    * </p>
    * 
    * @param data 源数据
    * @param publicKey 公钥(BASE64编码)
    * @return
    * @throws Exception
    */
   public static byte[] encryptByPublicKey(byte[] data, String publicKey)
   {

        try {
            byte[] keyBytes = Base64Utils.decode(publicKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Key publicK = keyFactory.generatePublic(x509KeySpec);
            // 对数据加密
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicK);
            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
   }

   /** *//**
    * <p>
    * 私钥加密
    * </p>
    * 
    * @param data 源数据
    * @param privateKey 私钥(BASE64编码)
    * @return
    * @throws Exception
    */
   public static byte[] encryptByPrivateKey(byte[] data, String privateKey)
   {
        try {
            byte[] keyBytes = Base64Utils.decode(privateKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateK);
            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return null;
   }

   /** *//**
    * <p>
    * 获取私钥
    * </p>
    * 
    * @param keyMap 密钥对
    * @return
    * @throws Exception
    */
   public static String getPrivateKey(Map<String, Object> keyMap)
   {
        try {
            Key key = (Key) keyMap.get(PRIVATE_KEY);
            return Base64Utils.encode(key.getEncoded());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       return null;
   }

   /** *//**
    * <p>
    * 获取公钥
    * </p>
    * 
    * @param keyMap 密钥对
    * @return
    * @throws Exception
    */
   public static String getPublicKey(Map<String, Object> keyMap)
   {
        try {
            Key key = (Key) keyMap.get(PUBLIC_KEY);
            return Base64Utils.encode(key.getEncoded());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
   }
  
   /** *//**
    * <p>
    * 获取私钥
    * </p>
    * 
    * @param privateKeystore 读取存储私钥的文件
    * @return
    * @throws Exception
    */
   public static String readPrivateKeyFile(String privateKeystore)
   {
       
        try {
            StringBuilder privateKey = new StringBuilder();
            FileReader fr = new FileReader(privateKeystore);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                privateKey.append(line);
            }
            br.close();
            fr.close();

            return privateKey.toString();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
   }
   
   /** *//**
    * <p>
    * 获取公钥
    * </p>
    * 
    * @param publicKeystore 存储公钥的文件
    * @return
    * @throws Exception
    */
   public static String readPublicKeyFile(String publicKeystore)
   {
        try {
            StringBuilder publicKey = new StringBuilder();
            FileReader fr = new FileReader(publicKeystore);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                publicKey.append(line);
            }
            br.close();
            fr.close();

            return publicKey.toString();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
   }
   
   /** *//**
    * <p>
    *  存储私钥到文件
    * </p>
    * 
    * @param privateKey      私钥字符串
    * @param privateKeystore 存储私钥的文件
    * @return
    * @throws Exception
    */
   public static void writePrivateKeyFile(String  privateKey, String privateKeystore)
   {
        try {
            FileWriter fw = new FileWriter(privateKeystore);
            fw.write(privateKey);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
   }
   
   /** *//**
    * <p>
    * 存储公钥到文件
    * </p>
    * 
    * @param publicKey 公钥文件
    * @param publicKeystore 存储公钥的文件
    * @return
    * @throws Exception
    */
   public static void writePublicKeyFile(String publicKey, String publicKeystore)
   {
        try {
            FileWriter fw = new FileWriter(publicKeystore);
            fw.write(publicKey);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
   }
   
   public static void testEncryptAndSignByPrivateKey(String publicKey, String privateKey) {
       System.err.println("私钥加密<===>公钥解密");
       String source = "1. 这是测试通过私钥加密，公钥解密的测试源数据" + 
                       "2. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "3. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "4. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "5. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "6. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "7. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "8. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "9. 这是测试通过私钥加密，公钥解密的测试源数据" +
                       "10. 这是测试通过私钥加密，公钥解密的测试源数据" 
                       ;
       System.out.println("加密前的数据：\r\n" + source);
       byte[] data = source.getBytes();
       try {
           
           byte[] encodeData = RSAUtils.encryptByPrivateKey(data, privateKey);
           System.out.println("加密后的数据：\r\n" + new String(encodeData));
           byte[] decodeData = RSAUtils.decryptByPublicKey(encodeData, publicKey);
           System.out.println("解密后的数据: \r\n" + new String(decodeData));
           System.err.println("私钥签名<===>公钥验签");
           String sign = RSAUtils.sign(decodeData, privateKey);
           System.out.println("签名数据: \r\n" + sign);
           boolean status = RSAUtils.verify(decodeData, publicKey, sign);
           System.out.println("验签结果：\r\n" + status);
           
       } catch (Exception e) {
            // TODO: handle exception
           e.printStackTrace();
       }
   }

public static void testEncryptByPublicKey(String publicKey, String privateKey) {
       System.err.println("公钥加密<===>私钥解密");
       String source = "这是测试公钥加密，私钥解密的测试源数据";
       System.out.println("加密前的数据：\r\n" + source);
//       byte[] data = source.getBytes();
       
       try {
           byte[] data = source.getBytes();
           byte[] encodedData = RSAUtils.encryptByPublicKey(data, publicKey);
           System.out.println("加密后的数据: \r\n" + new String(encodedData));
           
           byte[] decodedData = RSAUtils.decryptByPrivateKey(encodedData, privateKey);
           System.out.println("解密后的数据: \r\n" + new String(decodedData));
       } catch (Exception e) {
        // TODO Auto-generated catch block
           e.printStackTrace();
       }
   }


   public static void main(String[] args) {
       
       String publicKey;
       String privateKey;
       
       try {
           
           Map<String, Object> keyMap = RSAUtils.genKeyPair();
           publicKey = RSAUtils.getPublicKey(keyMap);
           privateKey = RSAUtils.getPrivateKey(keyMap);
           System.err.println("PublicKey: \r\n" + publicKey);
           System.err.println("PrivateKey: \r\n" + privateKey);
           
           Thread.sleep(100);
           testEncryptByPublicKey(publicKey, privateKey);
           Thread.sleep(100);
           testEncryptAndSignByPrivateKey(publicKey, privateKey);
           
           System.err.println("测试存储公钥文件: ");
           String publicKeystore = "D:\\test\\RSA\\publicKey.store";
           writePublicKeyFile(publicKey, publicKeystore);
           System.out.println("write ok");
           Thread.sleep(100);
           System.err.println("测试读取公钥文件: ");
           String readPublicKey = readPublicKeyFile(publicKeystore);
           System.out.println("读取的公钥文件：\r\n" + readPublicKey);
           
       } catch (Exception e) {
        // TODO: handle exception
           e.printStackTrace();
       }
           
   }

}

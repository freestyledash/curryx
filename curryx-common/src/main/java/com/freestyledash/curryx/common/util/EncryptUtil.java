package com.freestyledash.curryx.common.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author zhangyanqi
 * @since 1.0 2017/11/8
 */
public class EncryptUtil {

    private static byte[] key = {97, -113, -68, 93, 127, -17, 49, 67};
    private static String keyGenName = null;

    static {
//        if (key == null) {
//            KeyGenerator keyGen = null;//密钥生成器
//            try {
//                keyGen = KeyGenerator.getInstance("DES");
//            } catch (NoSuchAlgorithmException e) {
//            }
//            keyGen.init(56);//初始化密钥生成器
//            SecretKey secretKey = keyGen.generateKey();//生成密钥
//            key = secretKey.getEncoded();//密钥字节数组
        keyGenName = "DES";
//        }
    }

    /**
     * 加密
     *
     * @param content
     * @return
     */
    public static byte[] encrypt(byte[] content) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);//恢复密钥
        Cipher cipher1 = Cipher.getInstance(keyGenName);//Cipher完成加密或解密工作类
        cipher1.init(Cipher.ENCRYPT_MODE, secretKey);//对Cipher初始化，加密模式
        byte[] cipherByte = cipher1.doFinal(content);//加密data
        return cipherByte;
    }

    /**
     * 解密
     *
     * @param content
     * @return
     */
    public static byte[] decode(byte[] content) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);//恢复密钥
        Cipher cipher = Cipher.getInstance(keyGenName);//Cipher完成加密或解密工作类
        cipher.init(Cipher.DECRYPT_MODE, secretKey);//对Cipher初始化，解密模式
        byte[] origin = cipher.doFinal(content);//解密data
        return origin;
    }

}

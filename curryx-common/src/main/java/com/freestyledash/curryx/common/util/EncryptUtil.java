package com.freestyledash.curryx.common.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author zhangyanqi
 * @since 1.0 2017/11/8
 */
public class EncryptUtil {

    private static byte[] key = null;
    private static String keyGenName = null;

    static {
        key = new byte[]{97, -113, -68, 93, 127, -17, 49, 67};  //秘钥
        keyGenName = "DES";
    }

    /**
     * 加密
     *
     * @param content 被加密的内容
     * @return 加密结果
     */
    public static byte[] encrypt(byte[] content) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);//恢复密钥
        Cipher cipher = Cipher.getInstance(keyGenName);//Cipher完成加密或解密工作类
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);//对Cipher初始化，加密模式
        byte[] cipherByte = cipher.doFinal(content);//加密data
        return cipherByte;
    }

    /**
     * 解密
     *
     * @param content 被解密内容
     * @return 解密结果
     */
    public static byte[] decode(byte[] content) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);//恢复密钥
        Cipher cipher = Cipher.getInstance(keyGenName);//Cipher完成加密或解密工作类
        cipher.init(Cipher.DECRYPT_MODE, secretKey);//对Cipher初始化，解密模式
        byte[] origin = cipher.doFinal(content);//解密data
        return origin;
    }

}

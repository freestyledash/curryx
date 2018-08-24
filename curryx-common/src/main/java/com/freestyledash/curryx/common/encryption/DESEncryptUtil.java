package com.freestyledash.curryx.common.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * DES加密工具
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/8
 */
@SuppressWarnings("ALL")
public class DESEncryptUtil implements EncryptUtil {

    private byte[] key = null;
    private String keyGenName = null;

    {
        //秘钥
        key = new byte[]{97, -113, -68, 93, 127, -17, 49, 67};
        keyGenName = "DES";
    }

    /**
     * 加密
     *
     * @param content 被加密的内容
     * @return 加密结果
     */
    @Override
    public byte[] encrypt(byte[] content) throws Exception {
        //恢复密钥
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);
        //Cipher完成加密或解密工作类
        Cipher cipher = Cipher.getInstance(keyGenName);
        //对Cipher初始化，加密模式
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        //加密data
        byte[] cipherByte = cipher.doFinal(content);
        return cipherByte;
    }

    /**
     * 解密
     *
     * @param content 被解密内容
     * @return 解密结果
     */
    @Override
    public byte[] decode(byte[] content)  throws Exception {
        //恢复密钥
        SecretKey secretKey = new SecretKeySpec(key, keyGenName);
        //Cipher完成加密或解密工作类
        Cipher cipher = Cipher.getInstance(keyGenName);
        //对Cipher初始化，解密模式
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        //解密data
        byte[] origin = cipher.doFinal(content);

        return origin;
    }

}

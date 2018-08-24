package com.freestyledash.curryx.common.encryption;

/**
 * 加密工具接口
 * 所有的加密工具需要实现改接口
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/8
 */
public interface EncryptUtil {

    /**
     * 加密
     *
     * @param content 被加密的内容
     * @return 加密结果
     * @throws Exception 加密失败
     */
    public byte[] encrypt(byte[] content) throws Exception;

    /**
     * 解密
     *
     * @param content 被解密内容
     * @return 解密结果
     * @throws Exception 解密失败
     */
    public byte[] decode(byte[] content) throws Exception;

}

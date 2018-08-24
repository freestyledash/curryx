package com.freestyledash.curryx.common.serialization;

/**
 * 序列化工具的接口
 * 序列化工具接口需要实现该接口
 *
 * @author zhangyanqi
 */
public interface SerializationUtil {


    /**
     * 将对象<code>message</code>序列化为字节数组
     *
     * @param toSerialize 要序列化的对象
     * @return 序列化后的字节数组
     */
    public byte[] serialize(Object toSerialize);

    /**
     * 将字节数组<code>data</code>反序列化为对象
     *
     * @param data  字节数组
     * @param clazz 对象类型的Class对象
     * @param <T>   对象的类型参数
     * @return 反序列化后的对象
     */
    public <T> T deserialize(byte[] data, Class<T> clazz);

}
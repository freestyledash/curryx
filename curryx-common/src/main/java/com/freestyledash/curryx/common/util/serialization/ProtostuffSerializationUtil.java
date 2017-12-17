package com.freestyledash.curryx.common.util.serialization;

import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将对象序列化为字节数组以及将字节数组反序列化为对象的工具类
 * 使用框架:protostuff
 */
public final class ProtostuffSerializationUtil implements SerializationUtil {

    private static final Map<Class<?>, Schema<?>> cachedSchemas; //

    private static final Objenesis objenesis;

    static {
        cachedSchemas = new ConcurrentHashMap<Class<?>, Schema<?>>();
        objenesis = new ObjenesisStd(true);
    }

    /**
     * 将对象<code>message</code>序列化为字节数组
     *
     * @param message 要序列化的对象
     * @return 序列化后的字节数组
     */
    @SuppressWarnings("unchecked")
    public byte[] serialize(Object message) {
        Class clazz = message.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
            Schema schema = getSchema(clazz);
            return GraphIOUtil.toByteArray(message, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 将字节数组<code>data</code>反序列化为对象
     *
     * @param data  字节数组
     * @param clazz 对象类型的Class对象
     * @param <T>   对象的类型参数
     * @return 反序列化后的对象
     */
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        T message = objenesis.newInstance(clazz);
        Schema<T> schema = getSchema(clazz);
        GraphIOUtil.mergeFrom(data, message, schema);
        return message;
    }

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) cachedSchemas.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clazz);
            cachedSchemas.put(clazz, schema);
        }
        return schema;
    }

}
package com.freestyledash.curryx.common.serialization;



import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * 使用Hessian的序列化工具
 *
 * @author zhangyanqi
 * @since 1.0 2017/12/21
 */
public class HessianSerializationUtil implements SerializationUtil {

    /**
     * 序列化
     *
     * @param toSerialize 将要被序列化的对象
     * @return 序列化之后的数组
     */
    @Override
    public byte[] serialize(Object toSerialize) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        byte[] data = null;
        try {
            out.startMessage();
            out.writeInt(1);
            out.writeObject(toSerialize);
            out.completeMessage();
            out.close();
            data = bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    /**
     * 返序列化
     *
     * @param data  被反序列化的数组
     * @param clazz 目标类型对象
     * @return
     */
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        Hessian2Input in = new Hessian2Input(bin);
        ArrayList list = new ArrayList();
        try {
            in.startMessage();
            int length = in.readInt();
            for (int i = 0; i < length; i++) {
                list.add(in.readObject());
            }
            in.completeMessage();
            in.close();
            bin.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Object o = list.get(0);
        if (o == null) {
            return null;
        }
        Class<?> aClass = o.getClass();
        if (aClass != clazz) {
            return null;
        }
        return (T) o;
    }

}

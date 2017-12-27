package com.freestyledash.curryx.common.protocol.codec;

import com.freestyledash.curryx.common.protocol.entity.RPCRequest;
import com.freestyledash.curryx.common.util.encryption.DESEncryptUtil;
import com.freestyledash.curryx.common.util.encryption.EncryptUtil;
import com.freestyledash.curryx.common.util.serialization.ProtostuffSerializationUtil;
import com.freestyledash.curryx.common.util.serialization.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将对象编码为字节数组
 *
 * @author zhangyanqi
 */
public class RPCEncoder extends MessageToByteEncoder {


    private static final Logger logger = LoggerFactory.getLogger(RPCEncoder.class);

    /**
     * 要编码的对象类型
     */
    private final Class<?> clazz;

    /**
     * 序列化工具
     */
    private SerializationUtil serializationUtil;

    /**
     * 加密工具
     */
    private EncryptUtil encryptUtil;

    public RPCEncoder(SerializationUtil serializationUtil, EncryptUtil encryptUtil, Class toEncodeType) {
        this.serializationUtil = serializationUtil;
        this.encryptUtil = encryptUtil;
        this.clazz = toEncodeType;
    }

    public RPCEncoder(EncryptUtil encryptUtil, Class toEncodeType) {
        this.encryptUtil = encryptUtil;
        this.serializationUtil = new ProtostuffSerializationUtil();
        this.clazz = toEncodeType;
    }

    public RPCEncoder(SerializationUtil serializationUtil, Class toEncodeType) {
        this.encryptUtil = new DESEncryptUtil();
        this.serializationUtil = serializationUtil;
        this.clazz = toEncodeType;
    }

    public RPCEncoder(Class toEncodeType) {
        this.encryptUtil = new DESEncryptUtil();
        this.serializationUtil = new ProtostuffSerializationUtil();
        this.clazz = toEncodeType;
    }


    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if (clazz.isInstance(o)) {
            byte[] buffer = serializationUtil.serialize(o);
            logger.debug("将({})类型的对象编码为长度为{}的字节数组", RPCRequest.class.getName(), buffer.length);
            byte[] encrypt = encryptUtil.encrypt(buffer);
            byteBuf.writeInt(encrypt.length);
            byteBuf.writeBytes(encrypt);
            logger.debug("将({})类型的对象加密编码为长度为{}的字节数组", RPCRequest.class.getName(), encrypt.length);
        } else {
            throw new IllegalStateException("非法的编码请求：不兼容的编码类型");
        }
    }

    public SerializationUtil getSerializationUtil() {
        return serializationUtil;
    }

    public void setSerializationUtil(SerializationUtil serializationUtil) {
        this.serializationUtil = serializationUtil;
    }

    public EncryptUtil getEncryptUtil() {
        return encryptUtil;
    }

    public void setEncryptUtil(EncryptUtil encryptUtil) {
        this.encryptUtil = encryptUtil;
    }
}

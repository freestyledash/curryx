package com.freestyledash.curryx.common.protocol.codec;

import com.freestyledash.curryx.common.util.EncryptUtil;
import com.freestyledash.curryx.common.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将对象编码为字节数组
 */
public class RPCEncoder extends MessageToByteEncoder {

    private static final Logger logger = LoggerFactory.getLogger(RPCEncoder.class);

    /**
     * 要编码的对象类型
     */
    private final Class<?> clazz;

    public RPCEncoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if (clazz.isInstance(o)) {
            byte[] buffer = SerializationUtil.serialize(o);
            logger.debug("将({})类型的对象编码为长度为{}的字节数组", clazz.getName(), buffer.length);
            byte[] encrypt = EncryptUtil.encrypt(buffer);//加密
            byteBuf.writeInt(encrypt.length);
            byteBuf.writeBytes(encrypt);
            logger.debug("将({})类型的对象加密编码为长度为{}的字节数组", clazz.getName(), encrypt.length);
        } else {
            throw new IllegalStateException("非法的编码请求：不兼容的编码类型" + clazz);
        }
    }
}

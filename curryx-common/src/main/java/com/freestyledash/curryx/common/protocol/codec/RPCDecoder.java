package com.freestyledash.curryx.common.protocol.codec;

import com.freestyledash.curryx.common.protocol.entity.RPCResponse;
import com.freestyledash.curryx.common.util.encryption.DESEncryptUtil;
import com.freestyledash.curryx.common.util.encryption.EncryptUtil;
import com.freestyledash.curryx.common.util.serialization.ProtostuffSerializationUtil;
import com.freestyledash.curryx.common.util.serialization.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 将数组解码为对象
 */
public class RPCDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RPCDecoder.class);

    /**
     * 要解码的对象类型
     */
    private final Class<?> clazz = RPCResponse.class;

    /**
     * 序列化工具
     */
    private SerializationUtil serializationUtil;

    /**
     * 加密工具
     */
    private EncryptUtil encryptUtil;


    public RPCDecoder(SerializationUtil serializationUtil, EncryptUtil encryptUtil) {
        this.serializationUtil = serializationUtil;
        this.encryptUtil = encryptUtil;
    }

    public RPCDecoder(EncryptUtil encryptUtil) {
        this.encryptUtil = encryptUtil;
        this.serializationUtil = new ProtostuffSerializationUtil();
    }

    public RPCDecoder(SerializationUtil serializationUtil) {
        this.encryptUtil = new DESEncryptUtil();
        this.serializationUtil = serializationUtil;
    }

    public RPCDecoder() {
        this.encryptUtil = new DESEncryptUtil();
        this.serializationUtil = new ProtostuffSerializationUtil();
    }


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        //传入的字节数组的头表示需要反序列化的字节数组的长度，用int也就是4个字节来表示，所以当可读的字节数小于4时直接返回
        if (byteBuf.readableBytes() < 4) {
            return;
        }

        byteBuf.markReaderIndex();

        //读取字节数组的长度（不包括头部的一个int）
        int length = byteBuf.readInt();
        //若头部的int值小于0显然传输过程中发生了错误，关闭连接
        if (length < 0) {
            channelHandlerContext.close();
        }
        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex();
            return;
        }
        //解码
        byte[] body = new byte[length];
        byteBuf.readBytes(body);
        byte[] decode = encryptUtil.decode(body);
        logger.debug("将长度为({})的字节数组为({})类型的对象", decode.length, clazz.getName());
        Object message = serializationUtil.deserialize(decode, clazz);
        list.add(message);

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

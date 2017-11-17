package com.freestyledash.curryx.common.protocol.codec;

import com.freestyledash.curryx.common.util.EncryptUtil;
import com.freestyledash.curryx.common.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class RPCDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RPCDecoder.class);

    /**
     * 要解码的对象类型
     */
    private Class<?> clazz;

    public RPCDecoder(Class<?> clazz) {
        this.clazz = clazz;
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
        byte[] decode = EncryptUtil.decode(body);
        logger.debug("将长度为({})的字节数组为({})类型的对象", decode.length, clazz.getName());
        Object message = SerializationUtil.deserialize(decode, clazz);
        list.add(message);

    }
}

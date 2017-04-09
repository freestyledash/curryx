package com.nov21th.curry.common.protocol.codec;

import com.nov21th.curry.common.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RPC通讯解码器，将字节数组解码为指定类型的对象
 *
 * @author 郭永辉
 * @since 1.0 2017/4/2.
 */
public class RPCDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RPCDecoder.class);

    /**
     * 要解码的对象类型
     */
    private Class<?> clazz;

    public RPCDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

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

        //若校验通过则读取反序列化的字节数组并将反序列化后的对象保存起来
        byte[] body = new byte[length];
        byteBuf.readBytes(body);
        Object message = SerializationUtil.deserialize(body, clazz);
        list.add(message);

        logger.debug("将长度为{}的字节数组解码为({})类型的对象", length, clazz.getName());
    }
}

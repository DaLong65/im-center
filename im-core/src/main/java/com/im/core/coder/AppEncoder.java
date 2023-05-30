package com.im.core.coder;

import com.alibaba.fastjson.JSON;
import com.im.core.model.ReplyBody;
import com.im.core.protocol.MessageHeader;
import com.im.core.protocol.MessageTypeEnum;
import com.im.core.protocol.RequestContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author dalong
 * @date 2022/4/12 21:01
 */
@Slf4j
public class AppEncoder extends MessageToByteEncoder<RequestContext> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RequestContext requestContext, ByteBuf byteBuf) throws Exception {
        MessageHeader messageHeader = requestContext.getMessageHeader();
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.findByType(messageHeader.getMessageType());
        if (Objects.requireNonNull(messageTypeEnum) == MessageTypeEnum.RESPONSE) {
            replayMessageEncoder(byteBuf, messageHeader, messageHeader);
        }
    }

    /**
     * 回复消息编码
     *
     * @param byteBuf
     * @param messageHeader
     * @param messageBody
     */
    private void replayMessageEncoder(ByteBuf byteBuf, MessageHeader messageHeader, Object messageBody) {
        log.info("[im-center][appMessageEncoder]repayMessage messageBody:{}", JSON.toJSONString(messageBody));
        ReplyBody replyBody = (ReplyBody) messageBody;
        commonHeaderSend(byteBuf, messageHeader);
        byte[] bodyData = replyBody.getBody();
        byteBuf.writeInt(bodyData.length);
        byteBuf.writeBytes(bodyData);
    }

    /**
     * 写入协议头信息
     *
     * @param byteBuf
     * @param messageHeader
     */
    private void commonHeaderSend(ByteBuf byteBuf, MessageHeader messageHeader) {
        byteBuf.writeShort(messageHeader.getMagic());
        byteBuf.writeByte(messageHeader.getVersion());
        byteBuf.writeByte(messageHeader.getSerialization());
        byteBuf.writeByte(messageHeader.getMessageType());
        byteBuf.writeByte(messageHeader.getStatus());
        byteBuf.writeLong(messageHeader.getRequestId());
    }
}

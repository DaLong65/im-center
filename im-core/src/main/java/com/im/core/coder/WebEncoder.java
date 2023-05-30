package com.im.core.coder;

import com.alibaba.fastjson.JSON;
import com.im.core.model.ReplyBody;
import com.im.core.model.WebSocketRespBody;
import com.im.core.protocol.MessageTypeEnum;
import com.im.core.protocol.RequestContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author dalong
 * @date 2022/4/12 21:03
 * <p>
 * webSocket编码
 */
@Slf4j
public class WebEncoder extends MessageToMessageEncoder<RequestContext> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RequestContext requestContext, List<Object> list) throws Exception {
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.findByType(requestContext.getMessageType());

        if (Objects.requireNonNull(messageTypeEnum) == MessageTypeEnum.RESPONSE) {
            replayMessageEncoder(list, requestContext.getMessageType(), requestContext.getBody());
        }
    }

    /**
     * 回复
     *
     * @param list
     * @param messageType
     * @param messageBody
     */
    private void replayMessageEncoder(List<Object> list, Integer messageType, Object messageBody) {
        ReplyBody replyBody = (ReplyBody) messageBody;
        WebSocketRespBody respBody = new WebSocketRespBody();
        respBody.setUid(replyBody.getUid());
        respBody.setCode(replyBody.getCode());
        respBody.setMessage(replyBody.getMessage());
        respBody.setType(messageType);
        respBody.setTimestamp(respBody.getTimestamp());
        list.add(new TextWebSocketFrame(JSON.toJSONString(respBody)));
    }
}

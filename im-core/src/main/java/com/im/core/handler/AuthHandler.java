package com.im.core.handler;

import com.im.core.model.BindBody;
import com.im.core.protocol.MessageBody;
import com.im.core.protocol.RequestContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dalong
 * @date 2022/4/18 18:19
 */
@Slf4j
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<RequestContext<MessageBody>> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RequestContext<MessageBody> messageBodyImRequestContext) throws Exception {
        try {
            MessageBody messageBody = messageBodyImRequestContext.getBody();
            if (messageBody instanceof BindBody) {
                //TODO:业务判断处理
            }
            //TODO：授权业务处理

            channelHandlerContext.fireChannelRead(messageBodyImRequestContext);
        } finally {
            channelHandlerContext.pipeline().remove(this);
        }
    }
}

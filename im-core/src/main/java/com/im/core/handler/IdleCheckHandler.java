package com.im.core.handler;

import com.im.core.constant.IdleConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author dalong
 * @date 2022/4/18 18:09
 * <p>
 * 空闲检测
 */
@Slf4j
public class IdleCheckHandler extends IdleStateHandler {

    public IdleCheckHandler() {
        super(IdleConstants.READER_IDLE_TIME, IdleConstants.WRITER_IDLE_TIME, IdleConstants.ALL_IDLE_TIME, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            ctx.close();
            return;
        }
        super.channelIdle(ctx, evt);
    }
}

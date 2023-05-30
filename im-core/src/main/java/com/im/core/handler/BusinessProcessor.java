package com.im.core.handler;

import com.im.core.protocol.RequestContext;
import io.netty.channel.Channel;

/**
 * @author dalong
 * @date 2022/4/12 18:24
 * <p>
 * server 业务处理
 */
public interface BusinessProcessor {

    /**
     * 处理客户端通过长链发送数据
     *
     * @param channel
     * @param requestContext
     */
    void process(Channel channel, RequestContext requestContext);
}

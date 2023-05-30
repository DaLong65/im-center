package com.im.core.session;

import io.netty.channel.Channel;

/**
 * @author DaLong
 * @date 2023/5/29 17:30
 */
public interface ChannelSession {

    /**
     * 注册绑定
     *
     * @param channel
     */
    void register(Channel channel);

    /**
     * 删除绑定
     *
     * @param uid
     */
    void unRegister(String uid);
}

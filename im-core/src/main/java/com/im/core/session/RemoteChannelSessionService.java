package com.im.core.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author DaLong
 * @date 2023/5/29 17:34
 */
@Slf4j
public class RemoteChannelSessionService implements ChannelSession {

    /**
     * redis 或 db 存储。预留
     */

    @Override
    public void register(Channel channel) {

    }

    @Override
    public void unRegister(String uid) {

    }

    public String getChannelId(String uid) {
        return "";
    }
}

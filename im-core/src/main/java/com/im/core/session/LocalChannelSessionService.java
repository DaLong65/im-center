package com.im.core.session;

import com.im.core.constant.ChannelAttrConstants;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author DaLong
 * @date 2023/5/29 17:31
 */
@Slf4j
public class LocalChannelSessionService extends ConcurrentHashMap<String, Channel> implements ChannelSession {

    @Override
    public void register(io.netty.channel.Channel channel) {
        String uid = this.getKey(channel);
        if (StringUtils.isNotBlank(uid) && channel.isActive()) {
            this.putIfAbsent(uid, channel);
        }
    }

    @Override
    public void unRegister(String uid) {
        if (StringUtils.isNotBlank(uid)) {
            this.remove(uid);
        }
    }

    /**
     * 获取Channel Key
     *
     * @param channel
     * @return
     */
    private String getKey(io.netty.channel.Channel channel) {
        return channel.attr(ChannelAttrConstants.UID).get();
    }


    /**
     * 通道ID
     *
     * @param uid
     * @return
     */
    public String getChannelId(String uid) {
        Channel channel = get(uid);
        return channel.id().asLongText();
    }
}

package com.works.service.handler;

import com.im.core.constant.ChannelAttrConstants;
import com.im.core.model.ReplyBody;
import com.im.core.protocol.ChannelTypeEnum;
import com.im.core.protocol.MessageHeader;
import com.im.core.protocol.MessageTypeEnum;
import com.im.core.protocol.RequestContext;
import com.im.core.session.LocalChannelSessionService;
import com.im.core.session.RemoteChannelSessionService;
import com.works.service.message.ClientBroadcastMessage;
import com.works.service.message.MessageHeaderBuilder;
import com.works.service.message.RedisSendBroadcastHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;

/**
 * @author dalong
 * @date 2022/4/18 19:44
 */
@Slf4j
public class BaseHandler {

    @Resource
    protected LocalChannelSessionService localChannelSessionService;
    @Resource
    protected RemoteChannelSessionService remoteChannelSessionService;
    @Resource
    protected RedisSendBroadcastHandler redisSendBroadcastHandler;

    private final transient ChannelFutureListener remover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            log.info("[im-center][ChannelFutureListener] channel close listener ..");
            channelFuture.removeListener(this);
            String uid = "";
            //local
            localChannelSessionService.unRegister(uid);
            //remote

            //本地local数据变更

            //发送广播下线
            ClientBroadcastMessage broadcastMessage = new ClientBroadcastMessage();
            //构造参数
            redisSendBroadcastHandler.unRegister(broadcastMessage);
        }
    };

    public void addUserSession(Channel channel) {
        //添加channel
        channel.closeFuture().addListener(this.remover);
        //本地
        localChannelSessionService.register(channel);
        //远程
        remoteChannelSessionService.register(channel);
    }

    public void replaySuccess(Channel channel, String channelType) {
        ReplyBody replyBody = new ReplyBody();
        /**
         * 基础返回信息构建
         */
        RequestContext<ReplyBody> requestContext = new RequestContext<>();
        requestContext.setMessageType(MessageTypeEnum.RESPONSE.getCode());

        if (ChannelTypeEnum.SOCKET.getType().equals(channelType)) {
            MessageHeader messageHeader = MessageHeaderBuilder.build(MessageTypeEnum.RESPONSE.getCode());
            requestContext.setMessageHeader(messageHeader);
        }

        requestContext.setBody(replyBody);

        if (channel.isActive() && channel.isWritable()) {
            channel.writeAndFlush(requestContext);
        }
    }

    public Long getChannelTimestamp(String uid) {
        Channel channel = localChannelSessionService.get(uid);
        String timestampStr = channel.attr(ChannelAttrConstants.TIMESTAMP).get();
        if (StringUtils.isEmpty(timestampStr)) {
            return 0L;
        }
        return Long.valueOf(timestampStr);
    }

    public void closeChannel(Channel channel) {
        if (null != channel && channel.isActive() && channel.isOpen()) {
            channel.close();
        }
    }
}

package com.works.service.configuration;

import com.google.common.collect.Maps;
import com.im.core.handler.BusinessProcessor;
import com.im.core.handler.IpFilterRuleHandler;
import com.im.core.protocol.MessageTypeEnum;
import com.im.core.protocol.RequestContext;
import com.im.core.session.LocalChannelSessionService;
import com.im.core.trigger.ServerTrigger;
import com.works.service.annotation.BusinessHandler;
import com.works.service.configuration.properties.ServerProperties;
import io.netty.channel.Channel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationContextEvent;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * @author DaLong
 * @date 2023/5/29 17:51
 */
public class ServerConfiguration implements BusinessProcessor, ApplicationListener<ApplicationContextEvent> {

    @Resource
    private ApplicationContext applicationContext;

    private final Map<String, BusinessProcessor> handlerMap = Maps.newConcurrentMap();


    @Bean(destroyMethod = "destroy")
    public ServerTrigger setImSocketServer(ServerProperties properties) {
        return new ServerTrigger.Builder()
                .setAppPort(properties.getSocketPort())
                .setWebPort(properties.getWebSocketPort())
                .setBusinessProcessor(this)
                .build();
    }

    @Override
    public void process(Channel channel, RequestContext requestContext) {
        if (null == requestContext.getBody()) {
            return;
        }
        BusinessProcessor handler = handlerMap.get(Objects.requireNonNull(MessageTypeEnum.findByType(requestContext.getMessageType())).getKey());
        if (null == handler) {
            return;
        }
        handler.process(channel, requestContext);
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        Map<String, BusinessProcessor> beans = applicationContext.getBeansOfType(BusinessProcessor.class);
        for (Map.Entry<String, BusinessProcessor> entry : beans.entrySet()) {
            BusinessProcessor handler = entry.getValue();
            BusinessHandler annotation = handler.getClass().getAnnotation(BusinessHandler.class);
            if (null != annotation) {
                handlerMap.put(annotation.key(), handler);
            }
        }
        applicationContext.getBean(ServerTrigger.class).bind();
    }

    @Bean
    public LocalChannelSessionService userSession() {
        return new LocalChannelSessionService();
    }

    @Bean
    public IpFilterRuleHandler ipFilterRuleHandler() {
        return new IpFilterRuleHandler();
    }
}

package com.works.service.configuration;

import com.works.constants.CommonConstants;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author dalong
 * @date 2022/4/18 21:13
 */
@Configuration
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class ImRedisConfiguration {

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListener registerBroadcastReceiverListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(registerBroadcastReceiverListener, new ChannelTopic(CommonConstants.BIND_MESSAGE_REDIS_TOPIC));
        return container;
    }

   /* @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory,
                                                        MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("test.*"));

        return container;
    }*/
}

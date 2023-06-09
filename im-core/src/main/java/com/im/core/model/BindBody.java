package com.im.core.model;

import com.im.core.protocol.MessageBody;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author dalong
 * @date 2022/4/17 08:16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BindBody extends MessageBody {

    /**
     * 用户uid
     */
    private String uid;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 客户端类型
     */
    private String clientType;


    /**
     * APP
     */
    private String appVersion;

    /**
     * 时间戳
     */
    private Long timestamp;
}

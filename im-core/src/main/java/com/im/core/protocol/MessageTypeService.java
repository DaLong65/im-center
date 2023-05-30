package com.im.core.protocol;

/**
 * @author dalong
 * @date 2022/4/17 00:00
 * <p>
 * 业务数据转换
 */
public interface MessageTypeService {

    /**
     * 字节流转换 （不同业务类型转化不同对象）
     *
     * @param serializationType
     * @param dataBytes
     * @return
     */
    RequestContext byteToMessage(int serializationType, byte[] dataBytes);

    /**
     * 数据转换（webSocket使用）
     *
     * @param serializationType
     * @param value
     * @return
     */
    RequestContext messageToMessage(int serializationType, String value);
}

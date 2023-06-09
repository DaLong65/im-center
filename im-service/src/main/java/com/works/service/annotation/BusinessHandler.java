package com.works.service.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author dalong
 * @date 2022/4/12 18:29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BusinessHandler {

    String key();
}

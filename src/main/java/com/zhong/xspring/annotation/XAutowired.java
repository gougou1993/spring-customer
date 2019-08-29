package com.zhong.xspring.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
/**
 * @author zhong
 * @date 2019年8月29日 上午11:21:52
 * 
 */
public @interface XAutowired {

	String value() default "";
}

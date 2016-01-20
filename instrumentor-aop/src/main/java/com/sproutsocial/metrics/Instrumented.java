package com.sproutsocial.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 10/17/14
 *
 * @author horthy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Instrumented {

    String name() default "";
    double errorThreshold() default Instrumentor.NO_THRESHOLD_DEFINED;


}

package com.sproutsocial.metrics;

import java.lang.reflect.Method;

import com.codahale.metrics.MetricRegistry;

/**
 * Created on 3/31/15
 *
 * get the name of a method, including the declaring class.
 *
 * @author horthy
 */
public final class Names {
    private Names(){}
    public static String forMethod(Method method) {
        final Class klass = method.getDeclaringClass();
        return MetricRegistry.name(klass, method.getName());
    }
}

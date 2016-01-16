package com.sproutsocial.metrics.errors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sproutsocial.metrics.Names;

/**
 * Created on 10/17/14
 *
 * @author horthy
 */
public class ErrorTrackingInterceptor implements MethodInterceptor {

    static final Logger logger = LoggerFactory.getLogger(ErrorTrackingInterceptor.class);

    public static final String ERRORS_SUFFIX = ".errors";
    private final Predicate<Throwable> filter;
    private final MetricRegistry metricRegistry;

    @Inject
    public ErrorTrackingInterceptor(
            Predicate<Throwable> filter,
            MetricRegistry metricRegistry
    ) {
        this.filter = filter;
        this.metricRegistry = metricRegistry;
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final String meterName = getMeterName(invocation);
        return tryInvocation(invocation, meterName);
    }

    private String getMeterName(MethodInvocation invocation) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Method method = invocation.getMethod();
        final ErrorMeter annotation = method.getAnnotation(ErrorMeter.class);
        String meterName = annotation.name();
        if (Strings.isNullOrEmpty(meterName)) {
            meterName = Names.forMethod(method);
        }
        return meterName + ERRORS_SUFFIX;
    }

    private Object tryInvocation(MethodInvocation invocation, String meterName) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            logger.debug("error interceptor caught exception {} {}", e.getClass(), e.getMessage());
            if (filter.test(e)) {
                logger.debug("error interceptor marking meter {} for {}", meterName, e.getClass());
                metricRegistry.meter(meterName).mark();
            }
            throw e;
        }
    }
}

package com.sproutsocial.metrics.timing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sproutsocial.metrics.Names;

/**
 * Created on 10/17/14
 *
 * @author horthy
 */
public class TimingInterceptor implements MethodInterceptor {

    private final MetricRegistry metricRegistry;

    @Inject
    public TimingInterceptor(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final String timerName = getTimerName(invocation);
        final Timer timer = metricRegistry.timer(timerName);
        return timeInvocation(invocation, timer);
        
    }

    private String getTimerName(MethodInvocation invocation) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Method method = invocation.getMethod();
        final MyTimer annotation = method.getAnnotation(MyTimer.class);

        String timerName = annotation.name();
        if (Strings.isNullOrEmpty(timerName)) {
            timerName = Names.forMethod(method);
        }

        return timerName;
    }


    private Object timeInvocation(MethodInvocation invocation, Timer timer) throws Throwable {
        final Timer.Context context = timer.time();
        try {
            return invocation.proceed();
        } finally {
            context.stop();
        }
    }
}

package com.sproutsocial.metrics;

import static com.sproutsocial.metrics.Names.name;


import java.lang.reflect.Method;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.base.Strings;
import com.google.inject.Inject;

/**
 * Created on 4/17/15
 *
 * @author horthy
 */
public class InstrumentingInterceptor implements MethodInterceptor {
    

    private final Instrumentor instrumentor;

    @Inject
    public InstrumentingInterceptor(
            Instrumentor instrumentor
    ) {
        this.instrumentor = instrumentor;
    }


    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final Method method = methodInvocation.getMethod();
        final Optional<Double> threshold = getErrorThreshold(method);
        final String name = getName(method);

        return instrumentor.callThrowably(
                methodInvocation::proceed,
                name,
                threshold
        );
    }

    private String getName(Method method) {
        final String name = method
                .getDeclaredAnnotation(Instrumented.class)
                .name();

        return Strings.isNullOrEmpty(name) ? name(method) : name;
    }

    private Optional<Double> getErrorThreshold(Method method) {
        final double threshold = method.getDeclaredAnnotation(Instrumented.class)
                .errorThreshold();
        return threshold == Instrumentation.NO_THRESHOLD_DEFINED ?
                Optional.empty() :
                Optional.of(threshold);
    }


}

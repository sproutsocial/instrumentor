package com.sproutsocial.metrics;

import static com.google.common.base.Preconditions.checkNotNull;


import java.util.function.Predicate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

/**
 * Created on 10/17/14
 *
 * 
 *  
 * @author horthy
 */
public class InstrumentedMethodAnnotations extends AbstractModule {

    private final InstrumentedMethodInterceptor interceptor;

    public InstrumentedMethodAnnotations(InstrumentedMethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public static InstrumentedMethodAnnotations fromRegistries(
            MetricRegistry metricRegistry, HealthCheckRegistry healthCheckRegistry
    ) {
        return fromRegistries(metricRegistry, healthCheckRegistry, any -> true);
    }
    
    public static InstrumentedMethodAnnotations fromRegistries(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> filter
    ) {
        checkNotNull(metricRegistry);
        checkNotNull(healthCheckRegistry);
        return new InstrumentedMethodAnnotations(
                new InstrumentedMethodInterceptor(metricRegistry, healthCheckRegistry, filter));
    }
    
    @Override
    protected void configure() {
        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(InstrumentedMethod.class),
                interceptor
        );
    }
}

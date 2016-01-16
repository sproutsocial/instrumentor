package com.sproutsocial.metrics;

import static com.google.common.base.Preconditions.checkNotNull;


import java.util.Optional;
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
public class InstrumentedAnnotations extends AbstractModule {

    private final InstrumentingInterceptor interceptor;

    public InstrumentedAnnotations(InstrumentingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public static InstrumentedAnnotations forRegistry(
            MetricRegistry metricRegistry
    ) {
        return forRegistries(metricRegistry, null);
    }

    public static InstrumentedAnnotations forRegistries(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry
    ) {
        return forRegistries(metricRegistry, healthCheckRegistry, any -> true);
    }
    
    public static InstrumentedAnnotations forRegistries(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> filter
    ) {
        checkNotNull(metricRegistry);
        return new InstrumentedAnnotations(
                new InstrumentingInterceptor(
                        metricRegistry,
                        Optional.ofNullable(healthCheckRegistry),
                        filter
                )
        );
    }
    
    @Override
    protected void configure() {
        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(Instrumented.class),
                interceptor
        );
    }
}

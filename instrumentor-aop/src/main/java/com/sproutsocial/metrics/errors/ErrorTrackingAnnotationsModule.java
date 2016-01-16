package com.sproutsocial.metrics.errors;

import java.util.function.Predicate;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;

/**
 * Created on 10/17/14
 * 
 * binds the @TrackErrors annotation, and tracks all errors
 * 
 * Pass in a custom filter to control what gets counted in the
 * interceptor's underlying {@link com.codahale.metrics.Meter}
 *
 * @author horthy
 */
public class ErrorTrackingAnnotationsModule extends AbstractModule {
    

    private final ErrorTrackingInterceptor interceptor;

    /**
     * * 
     * @param metricsRegistryProvider Guice module that should provide
     *                                an instance of {@link com.codahale.metrics.MetricRegistry}
     * @param filter
     */
    public ErrorTrackingAnnotationsModule(
            Module metricsRegistryProvider,
            Predicate<Throwable> filter
    ) {
        this.interceptor = Guice.createInjector(
                metricsRegistryProvider,
                binder -> binder.bind(new TypeLiteral<Predicate<Throwable>>() {})
                                .toInstance(filter)
        ).getInstance(ErrorTrackingInterceptor.class);
    }


    public ErrorTrackingAnnotationsModule(Module metricsRegistryProvider) {
        this(metricsRegistryProvider, any -> true);
    }

    @Override
    protected void configure() {
        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(ErrorMeter.class),
                interceptor
        );
    }
}

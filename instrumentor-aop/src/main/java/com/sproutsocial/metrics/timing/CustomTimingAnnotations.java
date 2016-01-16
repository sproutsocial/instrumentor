package com.sproutsocial.metrics.timing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.matcher.Matchers;

/**
 * Created on 10/17/14
 *
 * @author horthy
 */
public class CustomTimingAnnotations extends AbstractModule{

    private final TimingInterceptor interceptor;

    public CustomTimingAnnotations(AbstractModule metricsRegistryProvider) {
        this.interceptor = Guice.createInjector(metricsRegistryProvider)
                                .getInstance(TimingInterceptor.class);
    }

    @Override
    protected void configure() {
        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(MyTimer.class),
                interceptor
        );
    }
}

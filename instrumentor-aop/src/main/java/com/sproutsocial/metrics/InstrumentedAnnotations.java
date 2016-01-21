package com.sproutsocial.metrics;

import java.util.function.Predicate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

/**
 * Created on 10/17/14
 *
 * @author horthy
 */
public class InstrumentedAnnotations extends AbstractModule {

    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;
    private final Predicate<Throwable> exceptionFilter;

    public static Builder builder() {
        return new Builder();
    }

    private InstrumentedAnnotations(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> exceptionFilter
    ) {
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
        this.exceptionFilter = exceptionFilter;
    }

    public InstrumentedAnnotations() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    @Override
    protected void configure() {

        bind(MetricRegistry.class).toInstance(metricRegistry);
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);

        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(Instrumented.class),
                new InstrumentingInterceptor(
                        new Instrumentor(
                                metricRegistry,
                                healthCheckRegistry,
                                exceptionFilter
                        )
                )
        );
    }

    public static class Builder {

        private MetricRegistry metricRegistry = new MetricRegistry();
        private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        private Predicate<Throwable> exceptionFilter = any -> true;

        private Builder(){}

        public Builder metricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        public Builder healthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
            this.healthCheckRegistry = healthCheckRegistry;
            return this;
        }

        public Builder exceptionFilter(Predicate<Throwable> exceptionFilter) {
            this.exceptionFilter = exceptionFilter;
            return this;
        }

        public InstrumentedAnnotations build() {
            return new InstrumentedAnnotations(metricRegistry, healthCheckRegistry, exceptionFilter);
        }
    }
}

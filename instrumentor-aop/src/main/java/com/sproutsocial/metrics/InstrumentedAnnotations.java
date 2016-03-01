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
        bindRegistries();
        bindInterceptors();
    }

    private void bindRegistries() {
        bind(MetricRegistry.class).toInstance(metricRegistry);
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);
    }

    private void bindInterceptors() {
        Instrumentor instrumentor = new Instrumentor(
                metricRegistry,
                healthCheckRegistry,
                exceptionFilter
        );

        bindInterceptor(
                Matchers.annotatedWith(Instrumented.class),
                Matchers.not(Matchers.annotatedWith(Instrumented.class)), // in case of both, defer to method annotation
                InstrumentingInterceptor.ofClasses(instrumentor)
        );

        bindInterceptor(
                Matchers.any(),
                Matchers.annotatedWith(Instrumented.class),
                InstrumentingInterceptor.ofMethods(instrumentor)
        );

    }

    public static class Builder {

        private MetricRegistry metricRegistry = new MetricRegistry();
        private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        private Predicate<Throwable> exceptionFilter = ExceptionFilters.markAllExceptions();

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

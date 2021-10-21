package com.sproutsocial.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class Instrumentor extends BaseInstrumentor {

    public Instrumentor() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    public Instrumentor(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> exceptionFilter) {
        super(metricRegistry, healthCheckRegistry, exceptionFilter);
    }

    @Override
    protected <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final InstrumentorContext context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()) {
                return callable.call();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.getErrorMeter().mark();
                }
                throw e;
            } finally {
                context.getInflightCounter().dec();
            }
        };
    }

    @Override
    protected <T> ThrowableCallable<T> instrumenting(
            ThrowableCallable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final InstrumentorContext context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()) {
                return callable.call();
            } catch (Throwable e) {
                if (exceptionFilter.test(e)) {
                    context.getErrorMeter().mark();
                }
                throw e;
            } finally {
                context.getInflightCounter().dec();
            }
        };
    }

    @Override
    protected CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final InstrumentorContext context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()) {
                runnable.run();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.getErrorMeter().mark();
                }
                throw e;
            } finally {
                context.getInflightCounter().dec();
            }
        };
    }

    @Override
    protected Runnable instrumenting(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final InstrumentorContext context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()) {
                runnable.run();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.getErrorMeter().mark();
                }
                throw e;
            } finally {
                context.getInflightCounter().dec();
            }
        };
    }

    @Override
    protected InstrumentorContext getInstrumentatorContext(String name) {
        return InstrumentorContext.buildDefaultInstrumentorContext(metricRegistry, name);
    }
}

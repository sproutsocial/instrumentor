package com.sproutsocial.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class ExtendedInstrumentor extends BaseInstrumentor{

    public ExtendedInstrumentor() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    public ExtendedInstrumentor(
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
            context.getTotalMeter().mark();
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()){
                T result = callable.call();
                context.getSuccessMeter().mark();
                return result;
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
            context.getTotalMeter().mark();
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()){
                T result = callable.call();
                context.getSuccessMeter().mark();
                return result;
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
            context.getTotalMeter().mark();
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()){
                runnable.run();
                context.getSuccessMeter().mark();
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
            context.getTotalMeter().mark();
            context.getInflightCounter().inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.getTimer().time()){
                runnable.run();
                context.getSuccessMeter().mark();
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
    protected InstrumentorContext createInstrumentationContext(String name) {
        return InstrumentorContext.buildExtendedInstrumentorContext(metricRegistry, name);
    }
}

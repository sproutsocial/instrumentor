package com.sproutsocial.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class InstrumentorContext {
    private Meter totalMeter;
    private Meter successMeter;
    private Meter errorMeter;
    private Timer timer;
    private Counter inflightCounter;

    public static InstrumentorContext buildDefaultInstrumentorContext(MetricRegistry metricRegistry, String name) {
        InstrumentorContext context = new InstrumentorContext();
        context.errorMeter = metricRegistry.meter(name + ".errors");
        context.timer = metricRegistry.timer(name);
        context.inflightCounter = metricRegistry.counter(name + ".inFlight");
        return context;
    }

    public static InstrumentorContext buildExtendedInstrumentorContext(MetricRegistry metricRegistry, String name) {
        InstrumentorContext context = buildDefaultInstrumentorContext(metricRegistry, name);
        context.totalMeter = metricRegistry.meter(name + ".total");
        context.successMeter = metricRegistry.meter(name + ".success");
        return context;
    }

    public Meter getTotalMeter() {
        return this.totalMeter;
    }

    public Meter getSuccessMeter() {
        return this.successMeter;
    }

    public Meter getErrorMeter() {
        return this.errorMeter;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public Counter getInflightCounter() {
        return this.inflightCounter;
    }
}

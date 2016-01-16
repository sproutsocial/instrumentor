package com.sproutsocial.metrics.healthchecks;

import java.util.Set;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableSet;

/**
 * Created on 4/19/15
 *
 * @author horthy
 */
public class GaugeHealthCheck<T extends Comparable<T>> extends HealthCheck {
    
    private final Gauge<T> gauge;
    private final T threshold;
    private final Set<T> alwaysHealthy;

    public GaugeHealthCheck(Gauge<T> gauge, T threshold, Set<T> alwaysHealthy) {
        this.gauge = gauge;
        this.threshold = threshold;
        this.alwaysHealthy = alwaysHealthy;
    }
    
    public GaugeHealthCheck(Gauge<T> gauge, T threshold) {
        this(gauge, threshold, ImmutableSet.of());
    }

    @Override
    protected Result check() throws Exception {
        final T value = gauge.getValue();
        
        // allow short-circuit
        if (alwaysHealthy.contains(value)) {
            return Result.healthy();
        }
        
        return value.compareTo(threshold) < 0 ?
                Result.healthy() :
                Result.unhealthy(getUnhealthyMessage(value));
    }

    private String getUnhealthyMessage(T value) {
        return "value=" + value + "&threshold=" + threshold;
    }
}

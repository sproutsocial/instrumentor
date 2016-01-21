package com.sproutsocial.metrics.gauges;

import java.util.function.Function;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;
import com.codahale.metrics.RatioGauge;

/**
 * Created on 4/19/15
 *
 * A {@link Gauge} that reports the
 * ratio of two {@link Metered} objects
 *
 * @author horthy
 */
public class MeteredRatioGauge extends RatioGauge {
    
    private final Metered numerator;
    private final Metered denominator;
    private final Function<Metered, Double> accessor;

    public MeteredRatioGauge(Metered numerator, Metered denominator, Function<Metered, Double> accessor) {
        this.numerator = numerator;
        this.denominator = denominator;
        this.accessor = accessor;
    }
    
    public MeteredRatioGauge(Metered numerator, Metered denominator) {
        this(numerator, denominator, Metered::getOneMinuteRate);
    }

    @Override
    protected Ratio getRatio() {
        return Ratio.of(
                accessor.apply(numerator),
                accessor.apply(denominator));
    }
}

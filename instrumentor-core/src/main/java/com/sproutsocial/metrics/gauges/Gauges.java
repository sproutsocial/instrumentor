package com.sproutsocial.metrics.gauges;

import java.util.function.Function;
import java.util.function.Supplier;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;
import com.codahale.metrics.RatioGauge;

/**
 * Created on 4/19/15
 *
 * @author horthy
 */
public final class Gauges {
    
    private Gauges(){}

    public static Gauge<Double> ratioOf(
            final Supplier<? extends Number> numerator,
            final Supplier<? extends Number> denominator
    ) {
        return new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(
                        numerator.get().doubleValue(),
                        denominator.get().doubleValue());
            }
        };
    }

    public static MeteredRatioGauge ratioOf(Metered numerator, Metered denominator) {
        return new MeteredRatioGauge(numerator, denominator);
    }


    public static MeteredRatioGauge ratioOf(
            Metered numerator,
            Metered denominator,
            Function<Metered, Double> accessor
    ) {
        return new MeteredRatioGauge(numerator, denominator, accessor);
    }
}

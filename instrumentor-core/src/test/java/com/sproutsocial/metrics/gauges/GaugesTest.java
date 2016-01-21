package com.sproutsocial.metrics.gauges;

import static org.junit.Assert.assertEquals;


import org.junit.Test;

import com.codahale.metrics.Gauge;

/**
 * Created on 8/31/15
 *
 * @author horthy
 */
public class GaugesTest {

    @Test
    public void testRatio() throws Exception {
        final Gauge<Double> gauge = Gauges.ratioOf(
                () -> 1.0d,
                () -> 4.0d
        );

        assertEquals((Double) 0.25d, gauge.getValue());
    }


}

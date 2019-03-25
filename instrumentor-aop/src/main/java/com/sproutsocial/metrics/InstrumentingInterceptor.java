package com.sproutsocial.metrics;

import java.lang.reflect.Method;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Created on 4/17/15
 *
 * @author horthy
 */
public class InstrumentingInterceptor implements MethodInterceptor {

    private final Instrumentor instrumentor;
    private final InstrumentationDetails instrumentationDetails;

    public InstrumentingInterceptor(
            Instrumentor instrumentor,
            InstrumentationDetails instrumentationDetails
    ) {
        this.instrumentor = instrumentor;
        this.instrumentationDetails = instrumentationDetails;
    }

    static InstrumentingInterceptor ofClasses(Instrumentor instrumentor) {
        return new InstrumentingInterceptor(
                instrumentor,
                new InstrumentationDetails.ClassInstrumentation()
        );
    }

    static InstrumentingInterceptor ofMethods(Instrumentor instrumentor) {
        return new InstrumentingInterceptor(
                instrumentor,
                new InstrumentationDetails.MethodInstrumentation()
        );
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final Method method = methodInvocation.getMethod();
        final Instrumented declaredAnnotation = instrumentationDetails.getAnnotation(method);
        if (declaredAnnotation == null) {
            // declaredAnnotation may be null, for example, when a class is @Instrumented, but it
            // inherits from an interface with default methods. In that case,
            // Method#getDeclaringClass will return the interface and not the implementing class. If
            // that interface is not also annotated with @Instrumented, then
            // instrumentationDetails#getAnnotation will return null.
            return methodInvocation.proceed();
        }
        final Optional<Double> threshold = getErrorThreshold(declaredAnnotation);
        final String name = instrumentationDetails.name(method, declaredAnnotation);

        return instrumentor.callThrowably(
                methodInvocation::proceed,
                name,
                threshold
        );
    }

    private Optional<Double> getErrorThreshold(Instrumented annotation) {
        final double threshold = annotation.errorThreshold();
        return threshold == Instrumentor.NO_THRESHOLD_DEFINED ?
                Optional.empty() :
                Optional.of(threshold);
    }

}

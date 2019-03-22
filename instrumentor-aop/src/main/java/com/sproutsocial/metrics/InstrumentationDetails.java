package com.sproutsocial.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;

import java.lang.reflect.Method;

/**
 * Created on 2/29/16.
 */
public interface InstrumentationDetails {

    Instrumented getAnnotation(Method method);
    String name(Method method, Instrumented annotation);

    class ClassInstrumentation implements InstrumentationDetails {

        @Override
        public Instrumented getAnnotation(Method method) {
            return method.getDeclaringClass().getAnnotation(Instrumented.class);
        }

        @Override
        public String name(Method method, Instrumented annotation) {

            String annotationName = annotation.name();

            if (Strings.isNullOrEmpty(annotationName)) {
                return Names.name(method);
            }
            return MetricRegistry.name(annotationName, method.getName());
        }
    }

    class MethodInstrumentation implements InstrumentationDetails {

        @Override
        public Instrumented getAnnotation(Method method) {
            return method.getDeclaredAnnotation(Instrumented.class);
        }

        @Override
        public String name(Method method, Instrumented annotation) {
            String annotationName = annotation.name();

            if (Strings.isNullOrEmpty(annotationName)) {
                return Names.name(method);
            }
            return annotationName;
        }
    }

}

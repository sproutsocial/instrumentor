Instrumentor
====================

Some handy instrumentation utilities for use with [Metrics](https://dropwizard.github.io/metrics/3.1.0/)


Instrumenting Methods 
---------------------

This metrics module's primary aim is to increase the ease with which you
can monitor calls to the key methods in your java application. If you're not sure
what methods to instrument or why you should be instrumenting anything
in the first place, you should go check out [Coda Hale's Metrics Introduction](https://youtu.be/czes-oa0yik?t=6m29s)

The module has a number of tools for instrumenting methods. In general, an 
instrumented method will report timing, call rate, error rates, and a count of
call in flight.

Assume we have the class

```java
package com.mycompany;

class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        new Example().sayHello();
    }
}

```

If we were to instrument the `sayHello` method, we'd get the following metrics registered and reported.


#### In Flight Count

You'll get a count of the current method calls in flight.

* `com.mycompany.Example.sayHello.inFlight.count` -- a count of method calls currently being executed

#### Call Rate

You'll get several stats for how often the `sayHello` method is being called.

* `com.mycompany.Example.sayHello.count` -- a lifetime count of calls to the method
* `com.mycompany.Example.sayHello.mean_rate` -- mean rate of calls
* `com.mycompany.Example.sayHello.m1_rate` -- rate of calls over the last 1 minute
* `com.mycompany.Example.sayHello.m5_rate` -- rate of calls over the last 5 minutes
* `com.mycompany.Example.sayHello.m15_rate` -- rate of calls over the last 15 minutes

#### Timing Statistics

You'll get a Histogram of the timing of your method. That is, a statistical
representation of the distribution of "How long did it take to call this method"

* `com.mycompany.Example.sayHello.max` -- maximum time it took to call the method
* `com.mycompany.Example.sayHello.mean` -- mean time to call the method
* `com.mycompany.Example.sayHello.min` -- minimum time it took to call the method
* `com.mycompany.Example.sayHello.p50` -- time it took for 50% of calls to complete
* `com.mycompany.Example.sayHello.p75` -- time it took for 75% of calls to complete
* `com.mycompany.Example.sayHello.p95` -- time it took for 95% of calls to complete
* `com.mycompany.Example.sayHello.p99` -- time it took for 99% of calls to complete
* `com.mycompany.Example.sayHello.p999` -- time it took for 99.9% of calls to complete
* `com.mycompany.Example.sayHello.stddev` -- standard deviation of method call times


#### Error Rate

You'll get stats to track the rate at which your method throws exceptions

* `com.mycompany.Example.sayHello.errors.count` -- a lifetime count of errors
* `com.mycompany.Example.sayHello.errors.mean_rate` -- mean rate of errors
* `com.mycompany.Example.sayHello.errors.m1_rate` -- rate of errors over the last 1 minute
* `com.mycompany.Example.sayHello.errors.m5_rate` -- rate of errors over the last 5 minutes
* `com.mycompany.Example.sayHello.errors.m15_rate` -- rate of errors over the last 15 minutes

#### Percent Error Rate
    
You'll get stats to track the percentage of method calls are throwing exceptions

* `com.mycompany.Example.sayHello.errors.total_pct` -- a lifetime percent of errors
* `com.mycompany.Example.sayHello.errors.mean_pct` -- mean percent of errors
* `com.mycompany.Example.sayHello.errors.m1_pct` -- percent of errors over the last 1 minute
* `com.mycompany.Example.sayHello.errors.m5_pct` -- percent of errors over the last 5 minutes
* `com.mycompany.Example.sayHello.errors.m15_pct` -- percent of errors over the last 15 minutes

#### HealthCheck

If you pass an error threshold, you'll get a HealthCheck that will monitor 
the percent of errors, and notify when the error rate exceeds the threshold.

Assuming the threshold given is `0.1` i.e. ten percent, and there were 
five errors in the last 100 calls, the healtcheck would report as "Healthy".

If there were, however, twenty errors in the last 100 calls, the healthcheck
would report as "Unhealthy", with a message of `"value=0.2&threshold=0.1"`

HealthChecks can by logged by interrogating the underlying `HealthCheckRegistry`.
Metrics has a good guide for HealthChecks [here](https://dropwizard.github.io/metrics/3.1.0/manual/healthchecks/).

The goal with healthchecks is to give us something that we can monitor to drive
pagerduty/nagios/zabbix/whatever we're using for monitoring and alerting.


**Note**

By default, the statistic used to determine the error percentage is `errors.m15_pct`.
This is not yet configurable, but its on the roadmap. The policy has been to lean toward
simplicity and sane defaults for now, and add configurability as it becomes clear what
needs to be configured.

How to Instrument
-----------------

#### Instrumenting with Instrumentor

The easiest way to instrument methods is using the `Instrumentor` class. Continuing
the example from earlier, lets instrument the `sayHello` method. To instrument
methods that return `void`, you'll want to use `Instrumentor#run`.

To build an Instrumentor, we'll need a `MetricRegistry` and a `HealthCheckRegistry`:

```java
    MetricRegistry metricRegistry = new MetricRegistry();
    HealthCheckRegistry metricRegistry = new MetricRegistry();
    Instrumentor instrumentor = new Instrumentor(metricRegistry, healthCheckRegistry);
```

To instrument the method, we'll need two things:

- an instance of `java.util.concurrent.Runnable`
- a `String` for the name of the method.


```java
package com.mycompany;

public class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
    
        // make an instance of Example
        final Example example = new Example();
        
        // build the instrumentor
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry metricRegistry = new MetricRegistry();
        Instrumentor instrumentor = new Instrumentor(metricRegistry, healthCheckRegistry);
        
        Runnable runnable =  new Runnable() {
            public void run() {
                example.sayHello(); 
            }
        };
        
        // run the method
        instrumentor.run(runnable, "com.mycompany.Example.sayHello");
    }
}
```


##### With a lambda

With java 8 lambdas, the last few lines become a bit simpler.

Instead of 

```java
        
        Runnable runnable =  new Runnable() {
            public void run() {
                example.sayHello(); 
            }
        };
        
        // run the method
        instrumentor.run(runnable, "com.mycompany.Example.sayHello");
}
```

we can use 

```java
        instrumentor.run(() -> example.sayHello(), "com.mycompany.Example.sayHello");
```

or even

```java
        instrumentor.run(example::sayHello, "com.mycompany.Example.sayHello");
```

##### Adding a healthcheck

To add a healthcheck, you need to supply an error threshold (`double`) as the third argument.

```java
        instrumentor.run(example::sayHello, "com.mycompany.Example.sayHello", 0.1);
```

##### Easier naming

If you're so inclined, you can use `MetricRegistry.name(Class<?> klass, String... names)`
to name your method:

```java
        instrumentor.run(
            example::sayHello,
            MetricRegistry.name(Example.class, "sayHello"),
            0.1
        );
```

##### Using Guice, ensuring you're using singleton registries

You should probably use guice to inject your MetricRegistry and HealthCheckRegistry.

Here's an example of a simple module that binds two registries and provides an instrumentor.

```java

class InstrumentorModule extends AbstractModule {

    public void configure() {
        bind(MetricRegistry.class).toInstance(new MetricRegistry());
        bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
        bind(new TypeLiteral<Predicate<Throwable>>() {}).toInstance(any -> true);
    }
    
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new InstrumentorModule());
        
        Instrumentor instrumentor = Injector.getInstance(Instrumentor.class);
        
        instrumentor.run(
            () -> System.out.println("Hello!"),
            "sayHello"
        );
        
        // same registry used by the instrumentor
        HealthCheckRegistry healthCheckRegistry = injector.getInstance(HealthCheckRegistry.class);
        
        HealthCheck.Result result = healthCheckRegistry.runHealthCheck("sayHello");
        
        assert result.isHealthy();
    }
}
```



**Note** To build an Instrumentor with Guice, you'll need to also bind a
`Predicate<Throwable>`, which will filter which exceptions get tracked.

For example, if you wanted to only track checked exceptions, you could

```java
bind(new TypeLiteral<Predicate<Throwable>>() {}).toInstance(e -> ! e instanceof RuntimeException);
```

In the example above, we bound the `Predicate` to `any -> true`, so that all exceptions will be tracked.

Instrumenting with Guice AOP
----------------------------

There are also modules for Instrumenting using annotations and
[Guice AOP](https://github.com/google/guice/wiki/AOP).

If you include an instance of `InstrumentedAnnotationsModule` in your
call to `Guice.createInjector` (see below), then you can use annotations
to instrument methods:


```java
package com.mycompany;

class Example {
    @InstrumentedMethod
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        
        Injector injector = Guice.createInjector(
            InstrumentedMethodAnnotations.fromRegistries(metricRegistry, healthCheckRegistry)
        );
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();
        
        Timer timer = metricRegistry.getMeters().get("com.mycompany.Example.sayHello");
        
        assert timer.getOneMinuteRate() > 0;
        
        Meter meter = metricRegistry.getMeters().get("com.mycompany.Example.sayHello.errors");
        
        assert meter.getOneMinuteRate() == 0;
    }
}
```

#### Method names

By default, the methods annotated by `@InstrumentedMethod` will be named

```
com.somepackage.ClassName.methodName
```

But that can be overriden using the `name` attribute of `InstrumentedMethod`:


```java
package com.mycompany;

class Example {
    @InstrumentedMethod(name="mySweetName")
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        
        Injector injector = Guice.createInjector(
            InstrumentedMethodAnnotations.fromRegistries(metricRegistry, healthCheckRegistry)
        );
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();
        
        Timer timer = metricRegistry.getMeters().get("mySweetName");
        
        assert timer.getOneMinuteRate() > 0;
        
        Meter meter = metricRegistry.getMeters().get("mySweetName.errors");
        
        assert meter.getOneMinuteRate() == 0;
    }
}
```

#### Health Check

To add a healthcheck for the error percentage, just add an `errorThreshold`
to the `@InstrumentedMethod` annotation.


```java
package com.mycompany;

class Example {
    @InstrumentedMethod(errorThreshold=0.1)
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        
        Injector injector = Guice.createInjector(
            InstrumentedMethodAnnotations.fromRegistries(metricRegistry, healthCheckRegistry)
        );
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();
        
        HealthCheck.Result result = healthCheckRegistry.runHealthCheck(
            "com.mycompany.Example.sayHello"
        );
        
        assert result.isHealthy();
    }
}
```

##### AOP Gotchas

Not seeing metrics that you think you should be? There 
are a couple of gotchas with guice AOP.

1. You'll need to make sure that the registries used to build the `InstrumentedMethodAnnotations`
module are the same as the ones you're reading from.

2. AOP only works for public methods being called by other classes. This is 
because Guice AOP works by inserting a proxy in front of the instrumented class
wherever it is injected. Calls of instrumented methods from within the same class
will not go through this proxy.

3. AOP will only be applied to objects created by guice. If you just use `new Example()`,
the annotation will be ignored.

Reporting Your Metrics
-----------------

Now that you're tracking metrics, you'll want to report them.

In dropwizard, metrics reporting can be configured in your yaml config, 
and you can inspect metrics and healthchecks using the admin servlet.

For non dropwizard applications, or to report to graphite see [The Metrics Documentation](https://dropwizard.github.io/metrics/3.1.0/manual/graphite/).

You can also embed your own [Admin Servlet](https://dropwizard.github.io/metrics/3.1.0/manual/servlets/).



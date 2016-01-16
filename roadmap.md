Figure out owner

MIT vs Apache license?
    guice + guava are apache

Remove package-statics, etc. Add ThrowableCallable instrumentor






Issues
========

Deploy to nexus and/or maven central
------------------------------------

Interface naming
-------------------
CheckedRunnable method name should be `runChecked`, not `run`.
Ditto `ThrowableThrowingCallable` if that guy makes it in.



Floored GaugeHealthCheck
-------------------

Current implementation of `GaugeHealthCheck` treats the threshold
as a ceiling. That is, if the gauge value is less than the threshold,
then the check will report as healthy. This is handy for things like error 



I think the best approach here is to rename the `T threshold` param/field to
`Optional<T> ceiling`. Once that is done, we can add an additional param/field, 
`Optional<T> floor`. This will not only grant more flexibility in the use of GaugeHealthCheck,
it will drive the addition of a new feature to `Instrumentor` and `@Instrument`: 
setting a minimum call rate for a service to be considered healthy.


#### Adding a minimum call rate to `@Instrument` and `Instrumentor#{call, callChecked, run, runChecked}`


Once we implement `floor`, we can also allow clients to specify a minimum call rate
for a method to be considered healthy.

We now have 4 potential parameters to `Instrumentor.{call, callChecked, run, runChecked}`,
it might be time to adopt some kind of builder pattern. 
For a regular old `Runnable`, this might look something like:

```java
instrumentor
    .named("com.mycompany.MyImportantClass.myImportantMethod") // name is required 
    .errorThreshold(0.01d)                                     // over 1% errors is unhealthy
    .minCallRate(1d)                                           // less than 1 call/second is unhealthy
    .instrumenting(this::myImportantMethod)
    .run()
```

Since we need a name to report metrics, only `Instrumentor#named` should be exposed on
the base class to begin building an instrumented `Runnable` or `Callable` etc.


#### Expanding the internals and API

The `.instrumenting()` finisher on the builder chain above could be extended to also allow
clients of this library to implement an interface rather than configure using a builder.

Depending on the argument to `instrumenting()`, this could return an object that implements either 

- `InstrumentedRunnable`
- `InstrumentedCheckedRunnable`
- `InstrumentedCallable`


This means that instead of using the builder returned
from `Instrumentor#named(String)` to build an instance of `Runnable`
a user could implement something like `InstrumentedRunnable`: 

```java
interface InstrumentedRunnable extends Runnable {

    void run();
    String name();

    default Optional<Double> errorThreshold()  {
        return Optional.empty();
    }

    default Optional<Double> minCallRate() {
        return Optional.empty();
    } 
}
```



#### Usage in `@Instrument`

This is much easier to implement in the AOP version, we just add 
another keyword argument to the annotation, and pull it out in `InstrumentingInterceptor`

```java
    @Instrument(name = "myMethod", errorThreshold = 0.01d, minCallRate = 1d)
    public void myMethod() {
        // do stuff 
    ]
```




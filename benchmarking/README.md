# J2CL Benchmarks

 <!-- TOC -->

## Running a benchmark for local changes

You can run benchmarks locally with
[`j2`](http://google3/CONTRIBUTING.md;l=41;rcl=381386450)
tool:

```
j2 bench {benchmark_name}
```

Where `{benchmark_name}` is the name of the benchmark.

TIP: You can run multiple benchmarks by passing multiple benchmark names or
`all` for running all benchmarks.

By default, `j2` will run the benchmark on all platforms (Closure, Wasm, JVM).
If you want to test a benchmark on a specific platform, you can use the `-p`
parameter of the `j2` tool:

```
j2 -p JVM bench {benchmark_name}
```

For running benchmarks for Web platforms, you would need to install v8 via
[jsvu](https://github.com/GoogleChromeLabs/jsvu) on your local machine.

## Debugging/Profiling a benchmark

A benchmark could be debugged or profiled by running the `_debug` target:

```
blaze run \
   //benchmarking/java/com/google/j2cl/benchmarks/octane:Box2dBenchmark_j2cl_debug
```

In addition while debugging J2CL benchmarks, you can add
`--define=J2CL_APP_STYLE=PRETTY` to have "pretty" output to help with profiling.

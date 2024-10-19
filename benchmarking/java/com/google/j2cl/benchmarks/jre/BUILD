load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmarks.bzl", "benchmark", "gen_benchmark_suite")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

benchmark(
    name = "ArrayIntCreateBenchmark",
)

benchmark(
    name = "ArrayIntSumBenchmark",
)

benchmark(
    name = "ArrayObjectCreateBenchmark",
)

benchmark(
    name = "ArrayObjectIterationBenchmark",
)

benchmark(
    name = "ArrayObjectSetBenchmark",
)

benchmark(
    name = "ArrayTestObjectCreateBenchmark",
)

benchmark(
    name = "ArrayTestObjectSetBenchmark",
)

benchmark(
    name = "LongAddBigNumbersBenchmark",
)

benchmark(
    name = "LongAddSmallNumbersBenchmark",
)

benchmark(
    name = "LongDivisionBigNumbersBenchmark",
)

benchmark(
    name = "LongDivisionSmallNumbersBenchmark",
)

benchmark(
    name = "LongMultiplyBigNumbersBenchmark",
)

benchmark(
    name = "LongMultiplySmallNumbersBenchmark",
)

benchmark(
    name = "LongShiftBigNumbersBenchmark",
)

benchmark(
    name = "LongShiftSmallNumbersBenchmark",
)

benchmark(
    name = "SmallExceptionBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "SmallExceptionWithStackTraceAccessedBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "BigExceptionBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "BigExceptionWithStackTraceAccessedBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "StringHashCodeBenchmark",
)

benchmark(
    name = "StringConcatIntBenchmark",
)

benchmark(
    name = "StringConcatDoubleBenchmark",
)

benchmark(
    name = "StringEqualsIgnoreCaseBenchmark",
    deps = ["//third_party:jsr305_annotations"],
)

benchmark(
    name = "StringEqualsIgnoreCaseNonAsciiBenchmark",
    deps = ["//third_party:jsr305_annotations"],
)

benchmark(
    name = "StringToUpperCaseBenchmark",
)

benchmark(
    name = "StringToUpperCaseNonAsciiBenchmark",
)

benchmark(
    name = "StringBuilderLongStringsBenchmark",
)

benchmark(
    name = "StringBuilderShortStringsBenchmark",
)

benchmark(
    name = "ArrayListObjectAddBenchmark",
)

benchmark(
    name = "ArrayListObjectGetBenchmark",
)

benchmark(
    name = "ArrayListObjectIterationBenchmark",
)

benchmark(
    name = "ArrayListObjectIterationInterfaceBenchmark",
)

benchmark(
    name = "ArrayListObjectSetBenchmark",
)

benchmark(
    name = "HashMapDoubleContainsKeyBenchmark",
)

benchmark(
    name = "HashMapDoublePutBenchmark",
)

benchmark(
    name = "HashMapObjectContainsKeyBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "HashMapObjectContainsValueBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "HashMapObjectIterationBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "HashMapObjectPutBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "HashMapStringContainsKeyBenchmark",
)

benchmark(
    name = "HashMapStringContainsValueBenchmark",
)

benchmark(
    name = "HashMapStringIterationBenchmark",
)

benchmark(
    name = "HashMapStringPutBenchmark",
)

benchmark(
    name = "LinkedHashMapStringContainsKeyBenchmark",
)

benchmark(
    name = "LinkedHashMapStringContainsValueBenchmark",
)

benchmark(
    name = "LinkedHashMapStringIterationBenchmark",
)

benchmark(
    name = "LinkedHashMapStringPutBenchmark",
)

benchmark(
    name = "TreeMapStringContainsKeyBenchmark",
)

benchmark(
    name = "TreeMapStringContainsValueBenchmark",
)

benchmark(
    name = "TreeMapStringIterationBenchmark",
)

benchmark(
    name = "TreeMapStringPutBenchmark",
)

benchmark(
    name = "SystemArrayCopyIntBenchmark",
)

benchmark(
    name = "SystemArrayCopyObjectBenchmark",
)

benchmark(
    name = "ArraysCopyOfIntBenchmark",
)

benchmark(
    name = "ArraysCopyOfObjectBenchmark",
)

benchmark(
    name = "ArraysSortIntBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "ArraysSortObjectBenchmark",
    deps = ["//benchmarking/java/com/google/j2cl/benchmarks/jre/helper:helpers"],
)

benchmark(
    name = "InstanceOfBenchmark",
    deps = ["//third_party:jsr305_annotations"],
)

benchmark(
    name = "InstanceOfDeepBenchmark",
)

benchmark(
    name = "DynamicClassDispatchBenchmark",
    deps = ["//third_party:jsr305_annotations"],
)

benchmark(
    name = "DynamicInterfaceDispatchBenchmark",
    deps = ["//third_party:jsr305_annotations"],
)

benchmark(
    name = "SandboxBenchmark",
)

gen_benchmark_suite(name = "jre")

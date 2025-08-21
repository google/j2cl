load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmarks.bzl", "benchmark", "gen_benchmark_suite")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

benchmark(
    name = "JdtCompileBenchmark",
    data = [
        "//samples/box2d/src/main/java:libbox2d_library-j2cl-src.jar",
    ],
    jvm_only = True,
    # Too slow to run as part of presubmits.
    perfgate_test_tags = [
        "manual",
        "notap",
        "guitar",
    ],
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler:tester_lib_public",
    ],
)

benchmark(
    name = "JavacCompileBenchmark",
    data = [
        "//samples/box2d/src/main/java:libbox2d_library-j2cl-src.jar",
    ],
    jvm_only = True,
    # Too slow to run as part of presubmits.
    perfgate_test_tags = [
        "manual",
        "notap",
        "guitar",
    ],
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler:tester_lib_public",
    ],
)

gen_benchmark_suite(name = "transpiler")

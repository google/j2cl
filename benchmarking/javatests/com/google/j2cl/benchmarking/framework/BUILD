load("//build_defs:rules.bzl", "j2cl_test")
load("@rules_java//java:defs.bzl", "java_test")
load("@rules_java//java:java_library.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

java_test(
    name = "BenchmarkFrameworkTest",
    srcs = ["BenchmarkFrameworkTest.java"],
    deps = [
        ":benchmark_framework_test_helpers",
        "//benchmarking/java/com/google/j2cl/benchmarking/framework",
        "//third_party:junit",
    ],
)

java_library(
    name = "benchmark_framework_test_helpers",
    testonly = True,
    srcs = [
        "MockBenchmark.java",
        "MockClock.java",
    ],
    deps = [
        "//benchmarking/java/com/google/j2cl/benchmarking/framework",
        "//third_party:junit",
    ],
)

j2cl_test(
    name = "BenchmarkFrameworkTest-j2cl",
    srcs = glob(["*.java"]),
    deps = [
        "//benchmarking/java/com/google/j2cl/benchmarking/framework:framework-j2cl",
        "//third_party:junit-j2cl",
    ],
)

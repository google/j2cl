load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmark_library.bzl", "benchmark_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//benchmarking/java/com/google/j2cl/benchmarks/octane:__pkg__",
    ],
    licenses = ["notice"],
)

benchmark_library(
    name = "navierstokes",
    srcs = glob(["*.java"]),
)

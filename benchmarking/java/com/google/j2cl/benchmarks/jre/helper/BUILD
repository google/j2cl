load("//:benchmarking/java/com/google/j2cl/benchmarking/benchmark_library.bzl", "benchmark_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//benchmarking:__subpackages__"],
    licenses = ["notice"],
)

benchmark_library(
    name = "helpers",
    srcs = glob(["*.java"]),
)

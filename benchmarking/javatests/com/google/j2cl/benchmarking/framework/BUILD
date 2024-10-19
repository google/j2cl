load("//build_defs:rules.bzl", "j2cl_test")
load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "BenchmarkFrameworkTest",
    srcs = glob(["*.java"]),
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

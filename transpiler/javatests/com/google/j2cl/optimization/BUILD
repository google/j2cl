# Description:
#   j2cl tests verifying important optimizations

load("//build_defs:rules.bzl", "j2cl_library")
load(":j2cl_optimization_test.bzl", "j2cl_optimization_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "shared",
    testonly = 1,
    srcs = ["OptimizationTestUtil.java"],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

j2cl_optimization_test(
    name = "BooleanOptimizationTest",
    javacopts = [
        "-XepDisableAllChecks",
    ],
)

j2cl_optimization_test(
    name = "StringOptimizationTest",
    javacopts = [
        "-XepDisableAllChecks",
    ],
)

j2cl_optimization_test(
    name = "ArrayOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

j2cl_optimization_test(
    name = "ArrayListOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

j2cl_optimization_test(
    name = "CastDisableOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

j2cl_optimization_test(
    name = "EnumOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

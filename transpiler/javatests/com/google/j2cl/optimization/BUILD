# Description:
#   j2cl tests verifying important optimizations

load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_test(
    name = "OptimizationTestSuite",
    srcs = glob(
        ["*.java"],
        exclude = ["OptimizationTestUtil.java"],
    ),
    compile = 1,
    compiler = "//javascript/tools/jscompiler:head",
    extra_defs = [
        "--rewrite_polyfills=false",
        "--strict",
        "--variable_renaming=OFF",
        "--define=jre.checkedMode=DISABLED",
        "--define=jre.checks.checkLevel=MINIMAL",
    ],
    javacopts = [
        "-XepDisableAllChecks",
    ],
    deps = [
        ":OptimizationTestUtil",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

j2cl_library(
    name = "OptimizationTestUtil",
    testonly = 1,
    srcs = ["OptimizationTestUtil.java"],
    visibility = [
        "//transpiler/javatests/com/google/j2cl/optimization:__subpackages__",
    ],
    deps = [
        "//third_party:junit-j2cl",
    ],
)

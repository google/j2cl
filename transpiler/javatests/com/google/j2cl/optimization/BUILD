# Description:
#   j2cl tests verifying important optimizations

load("//build_defs:rules.bzl", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_test(
    name = "OptimizationTestSuite",
    srcs = glob(["*.java"]),
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
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

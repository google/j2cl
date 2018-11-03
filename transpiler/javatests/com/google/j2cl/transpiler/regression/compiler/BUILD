# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

j2cl_library(
    name = "CompilerSuiteLib",
    testonly = 1,
    srcs = glob(["**/*.java"]),
    javacopts = [
        "-XepDisableAllChecks",
    ],
    native_srcs = ["ArraysTest.native.js"],
    deps = [
        "//third_party/java/gwt:gwt-javaemul-internal-annotations-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/junit:junit-j2cl",
        "//third_party/java/truth:truth-j2cl",
    ],
)

j2cl_test(
    name = "CompilerSuite_uncompiled",
    shard_count = 8,
    test_class = "com.google.j2cl.transpiler.regression.compiler.CompilerSuite",
    runtime_deps = [":CompilerSuiteLib"],
)

j2cl_test(
    name = "CompilerSuite_compiled",
    compile = 1,
    shard_count = 8,
    test_class = "com.google.j2cl.transpiler.regression.compiler.CompilerSuite",
    runtime_deps = [":CompilerSuiteLib"],
)

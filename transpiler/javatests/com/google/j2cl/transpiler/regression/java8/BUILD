# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

j2cl_library(
    name = "Java8TestLib",
    testonly = 1,
    srcs = glob(["**/*.java"]),
    javacopts = [
        "-Xep:EqualsIncompatibleType:OFF",
        "-Xep:SelfComparison:OFF",  # See go/self-comparison-lsc
        "-Xep:SelfEquals:OFF",  # See go/self-equals-lsc
        "-Xep:SelfEquality:OFF",
        "-Xep:LoopConditionChecker:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
        "-Xep:ClassCanBeStatic:OFF",
        "-Xep:MethodCanBeStatic:OFF",
    ],
    native_srcs = ["Java8Test.native.js"],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/junit:junit-j2cl",
        "//third_party/java/truth:truth-j2cl",
    ],
)

j2cl_test(
    name = "Java8Test_uncompiled",
    test_class = "com.google.j2cl.transpiler.regression.java8.Java8Test",
    runtime_deps = [":Java8TestLib"],
)

j2cl_test(
    name = "Java8Test_compiled",
    compile = 1,
    test_class = "com.google.j2cl.transpiler.regression.java8.Java8Test",
    runtime_deps = [":Java8TestLib"],
)

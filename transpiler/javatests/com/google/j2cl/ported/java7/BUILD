load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "Java7TestLib",
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
    deps = [
        "//third_party:junit-j2cl",
        "//third_party:truth-j2cl",
    ],
)

j2cl_test(
    name = "Java7Test_uncompiled",
    test_class = "com.google.j2cl.ported.java7.Java7Test",
    runtime_deps = [":Java7TestLib"],
)

j2cl_test(
    name = "Java7Test_compiled",
    compile = 1,
    test_class = "com.google.j2cl.ported.java7.Java7Test",
    runtime_deps = [":Java7TestLib"],
)

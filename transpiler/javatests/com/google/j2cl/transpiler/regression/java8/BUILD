# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_test")

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

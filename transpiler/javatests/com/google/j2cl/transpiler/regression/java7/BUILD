# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_test")

j2cl_test(
    name = "Java7Test_uncompiled",
    test_class = "com.google.j2cl.transpiler.regression.java7.Java7Test",
    runtime_deps = [":Java7TestLib"],
)

j2cl_test(
    name = "Java7Test_compiled",
    compile = 1,
    test_class = "com.google.j2cl.transpiler.regression.java7.Java7Test",
    runtime_deps = [":Java7TestLib"],
)

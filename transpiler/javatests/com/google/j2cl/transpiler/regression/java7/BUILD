package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//build_def:j2cl_test.bzl", "j2cl_test")

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

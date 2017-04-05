package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")

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

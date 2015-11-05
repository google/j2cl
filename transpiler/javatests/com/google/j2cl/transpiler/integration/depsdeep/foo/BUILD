package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/build_def/j2cl_java_library",
    "j2cl_java_library",
)

j2cl_java_library(
    name = "foo_lib",
    srcs = ["Foo.java"],
    deps = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsdeep/bar:bar_lib"],
)

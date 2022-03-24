load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "foo",
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
    deps = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/bar"],
)

load("//build_defs:rules.bzl", "j2cl_library")

package(licenses = ["notice"])

j2cl_library(
    name = "foo-j2cl",
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
    deps = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/bar:bar-j2cl"],
)

java_library(
    name = "foo",
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
    deps = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/bar"],
)

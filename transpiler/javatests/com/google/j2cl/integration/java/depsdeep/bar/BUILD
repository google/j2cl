load("//build_defs:rules.bzl", "j2cl_library")

package(licenses = ["notice"])

j2cl_library(
    name = "bar-j2cl",
    srcs = ["Bar.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
)

java_library(
    name = "bar",
    srcs = ["Bar.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
)

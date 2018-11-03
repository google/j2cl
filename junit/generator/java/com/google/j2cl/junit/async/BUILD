# Hidden from production use since don't want to expose crossplatform async
# testing under j2cl.
# JVM async testing support should be placed under Guava or Xplat.

package(default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit:__subpackages__"])

# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

java_library(
    name = "async",
    testonly = 1,
    srcs = ["J2clAsyncTestRunner.java"],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
    ],
)

j2cl_library(
    name = "async-j2cl",
    testonly = 1,
    srcs = ["super/J2clAsyncTestRunner.java"],
    deps = [
        "//third_party/java/junit:junit-j2cl",
    ],
)

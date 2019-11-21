# Hidden from production use since don't want to expose crossplatform async
# testing under j2cl.
# JVM async testing support should be placed under Guava or Xplat.

load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_visibility = [
        "//:__pkg__",
        "//junit/generator/javatests/com/google/j2cl/junit:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

java_library(
    name = "async",
    testonly = 1,
    srcs = [
        "AsyncTestRunner.java",
    ],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
    ],
)

j2cl_library(
    name = "async-j2cl",
    testonly = 1,
    srcs = [
        "super/AsyncTestRunner.java",
    ],
    deps = [
        "//third_party:junit-j2cl",
    ],
)

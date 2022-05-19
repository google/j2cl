# Hidden from production use since don't want to expose crossplatform async
# testing under j2cl.
# JVM async testing support should be placed under Guava or Xplat.

load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_visibility = [
        "//:__pkg__",
        "//junit/generator:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "async",
    testonly = 1,
    srcs = [
        "AsyncTestRunner.java",
        "Timeout.java",
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
        "Timeout.java",
        "super/AsyncTestRunner.java",
    ],
    deps = [
        "//third_party:junit-j2cl",
    ],
)

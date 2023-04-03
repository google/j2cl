load(
    "//junit/generator/javatests/com/google/j2cl/junit/integration:j2cl_junit_integration_test.bzl",
    "java_and_j2cl_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration:__subpackages__"],
    # Tests in this directory are not tests that are meant to be run directly.
    # They serve as integration tests for J2clTestRunner.
    licenses = ["notice"],
)

# test is used as an executable to test j2cl's test runner
java_and_j2cl_library(
    name = "timer",
    srcs = ["Timer.java"],
    super_srcs = ["super/Timer.java"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

java_and_j2cl_library(
    name = "thenable",
    srcs = ["Thenable.java"],
    deps = ["//third_party:gwt-jsinterop-annotations"],
)

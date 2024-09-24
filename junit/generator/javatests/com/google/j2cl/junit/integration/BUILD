load("@rules_java//java:defs.bzl", "java_library", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "junit_integration_helper",
    testonly = 1,
    srcs = glob(["*.java"]),
    visibility = [":__subpackages__"],
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//third_party:junit",
        "//third_party:truth",
    ],
)

java_test(
    name = "StacktraceAsserterTest",
    srcs = ["StacktraceAsserterTest.java"],
    deps = [
        ":junit_integration_helper",
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:truth",
    ],
)

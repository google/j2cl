load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "J2clTestSuiteToolTest",
    srcs = ["J2clTestSuiteToolTest.java"],
    deps = [
        "//third_party:junit",
        "//tools/java/com/google/j2cl/tools/junit:j2cl_test_suite_tool_lib",
    ],
)

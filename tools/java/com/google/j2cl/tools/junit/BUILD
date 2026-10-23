load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_binary(
    name = "j2cl_test_suite_tool",
    testonly = True,
    main_class = "com.google.j2cl.tools.junit.J2clTestSuiteTool",
    visibility = ["//visibility:public"],
    runtime_deps = [":j2cl_test_suite_tool_lib"],
)

java_library(
    name = "j2cl_test_suite_tool_lib",
    testonly = True,
    srcs = ["J2clTestSuiteTool.java"],
    visibility = ["//tools/javatests/com/google/j2cl/tools/junit:__pkg__"],
)

# Apache2. These tests are written for J2CL emulation, not copied over.
licenses(["notice"])

load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")

j2cl_test(
    name = "junit_framework_AssertTest",
    srcs = ["junit/framework/AssertTest.java"],
    test_class = "junit.framework.AssertTest",
    deps = ["//third_party/java/junit:junit-j2cl"],
)

j2cl_test(
    name = "org_junit_AssertTest",
    srcs = ["org/junit/AssertTest.java"],
    test_class = "org.junit.AssertTest",
    deps = ["//third_party/java/junit:junit-j2cl"],
)

load("//build_defs:rules.bzl", "j2cl_test", "j2wasm_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    # Apache2. These tests are written for J2CL emulation, not copied over.
    licenses = ["notice"],
)

j2cl_test(
    name = "junit_framework_AssertTest",
    srcs = ["junit/framework/AssertTest.java"],
    test_class = "junit.framework.AssertTest",
    deps = ["//third_party:junit-j2cl"],
)

j2cl_test(
    name = "org_junit_AssertTest",
    srcs = ["org/junit/AssertTest.java"],
    test_class = "org.junit.AssertTest",
    deps = ["//third_party:junit-j2cl"],
)

j2wasm_test(
    name = "junit_framework_AssertTest-j2wasm",
    srcs = ["junit/framework/AssertTest.java"],
    test_class = "junit.framework.AssertTest",
    deps = ["//third_party:junit-j2wasm"],
)

j2wasm_test(
    name = "org_junit_AssertTest-j2wasm",
    srcs = ["org/junit/AssertTest.java"],
    test_class = "org.junit.AssertTest",
    deps = ["//third_party:junit-j2wasm"],
)

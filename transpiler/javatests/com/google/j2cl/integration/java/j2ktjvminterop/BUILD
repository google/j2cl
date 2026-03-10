load("//build_defs:rules.bzl", "j2cl_library")
load("@rules_java//java:defs.bzl", "java_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

JAVA_SRCS = ["Java*.java"]

java_library(
    name = "java",
    srcs = glob(JAVA_SRCS),
)

# Use untranspiled Java for the J2KT-JVM variant of this test (we want to test Java interop)
alias(
    name = "java-j2kt-jvm",
    actual = ":java",
)

# Transpile other j2* platforms.
j2cl_library(
    name = "java-j2cl",
    srcs = glob(JAVA_SRCS),
)

integration_test(
    srcs = glob(
        ["*.java"],
        exclude = JAVA_SRCS,
    ),
    enable_kt_native = False,
    enable_wasm = False,
    deps = [":java"],
)

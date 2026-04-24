load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "Utf8ByteOffsetConverterTest",
    srcs = ["Utf8ByteOffsetConverterTest.java"],
    deps = [
        "//third_party:junit",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)

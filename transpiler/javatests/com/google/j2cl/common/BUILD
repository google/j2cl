load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "SourceUtilsTest",
    srcs = ["SourceUtilsTest.java"],
    deps = [
        "//third_party:junit",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/common",
    ],
)

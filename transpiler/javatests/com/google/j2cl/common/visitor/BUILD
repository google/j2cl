load("@rules_java//java:defs.bzl", "java_library", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

java_library(
    name = "test_nodes",
    testonly = 1,
    srcs = ["TestNodes.java"],
    deps = [
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common/visitor",
    ],
)

java_test(
    name = "VisitorTest",
    srcs = ["VisitorTest.java"],
    deps = [
        ":test_nodes",
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/common/visitor",
    ],
)

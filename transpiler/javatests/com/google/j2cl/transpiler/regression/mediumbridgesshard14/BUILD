load("//build_defs:rules.bzl", "j2cl_test")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_test(
    name = "TestShard",
    srcs = [
        "TestCases.srcjar",
        "TestShard.java",
    ],
    tags = ["manual"],
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:junit-j2cl",
    ],
)

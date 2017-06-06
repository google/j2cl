package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load("//build_def:j2cl_test.bzl", "j2cl_test")

j2cl_test(
    name = "TestShard",
    srcs = [
        "TestCases.srcjar",
        "TestShard.java",
    ],
    tags = ["manual"],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/junit:junit-j2cl",
    ],
)

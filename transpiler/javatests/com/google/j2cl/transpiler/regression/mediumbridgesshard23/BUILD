package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")

j2cl_test(
    name = "TestShard",
    srcs = [
        "TestCases.srcjar",
        "TestShard.java",
    ],
    tags = [
        "manual",
        "notap",  #TODO(b/38001177): re-enable when the test passes.
    ],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/junit:junit-j2cl",
    ],
)

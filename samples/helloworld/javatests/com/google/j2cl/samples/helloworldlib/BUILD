# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_test")

j2cl_test(
    name = "HelloWorldTest",
    srcs = glob(["*.java"]),
    deps = [
        "//samples/helloworld/java/com/google/j2cl/samples/helloworldlib",
        "//third_party:junit-j2cl",
    ],
)

licenses(["notice"])  # Apache 2.0

load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_test")

j2cl_test(
    name = "HelloWorldTest",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/j2cl/samples/helloworldlib",
        "@com_google_j2cl//third_party:junit-j2cl",
    ],
)

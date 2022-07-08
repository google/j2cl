load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    build_kt_readables = False,
    deps = ["//jre/java:javaemul_internal_annotations-j2cl"],
)

load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["**/*.java"]),
    # TODO(b/466394693): make the normalization work with j2kt.
    build_kt_readables = False,
    experimental_java_frontend = "javac",
    javacopts = [
        "-source 21",
        "-target 21",
    ],
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

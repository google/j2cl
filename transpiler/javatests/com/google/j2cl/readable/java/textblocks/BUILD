load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.java"]),
    experimental_java_frontend = "javac",
    javacopts = [
        "-source 21",
        "-target 21",
        # Allow trailing whitespace in text blocks
        "-Xlint:-text-blocks",
    ],
)

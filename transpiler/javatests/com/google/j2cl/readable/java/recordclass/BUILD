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
    experimental_java_frontend = "javac",
    # TODO(b/445545563): Support record -> data class transpilation.
    generate_kt_readables = False,
    javacopts = [
        "-source 21",
        "-target 21",
    ],
)

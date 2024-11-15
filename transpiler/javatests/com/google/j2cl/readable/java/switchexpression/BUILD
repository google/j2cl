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
    # TODO(b/375058137): Enable when bug is fixed.
    generate_kt_readables = False,
    # TODO(b/375058020): Enable when bug is fixed.
    generate_wasm_readables = False,
    javacopts = [
        "-source 14",
        "-target 14",
    ],
)

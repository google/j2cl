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
    generate_kt_readables = False,
    generate_readable_source_maps = True,
    generate_wasm_readables = False,
    javacopts = [
        "-source 14",
        "-target 14",
    ],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

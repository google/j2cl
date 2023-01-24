load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/264466634): Reanable when imports are auto generated.
    generate_wasm_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

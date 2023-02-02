load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # JS properties not supported in Kotlin
    generate_kt_readables = False,
    # JS properties on non-native types not supported in WASM.
    generate_wasm_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

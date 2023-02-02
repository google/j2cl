load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # JS method not supported in Kotlin
    generate_kt_readables = False,
    generate_wasm_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

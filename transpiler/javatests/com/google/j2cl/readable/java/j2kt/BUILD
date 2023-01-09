load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["**/*.java"]),
    generate_js_readables = False,
    generate_wasm_readables = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:j2objc_annotations-j2cl",
    ],
)

load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    generate_js_readables = False,
    generate_wasm_readables = False,
    deps = [
        "//third_party:j2objc_annotations-j2cl",
        "//transpiler/javatests/com/google/j2cl/readable/java/j2ktobjcname/foowithpackageinfo:foowithpackageinfo-j2cl",
    ],
)

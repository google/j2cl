load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    generate_kt_readables = True,
    wasm_entry_points = [
        r"emptymethod\.EmptyMethod\.main",
    ],
)

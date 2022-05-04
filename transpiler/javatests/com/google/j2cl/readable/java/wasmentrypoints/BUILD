load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    wasm_entry_points = [
        r"wasmentrypoints\.WasmEntryPoints\.singleMatchedEntryPoint",
        r".*\.matchedEntryPointPattern[1-2]",
    ],
)

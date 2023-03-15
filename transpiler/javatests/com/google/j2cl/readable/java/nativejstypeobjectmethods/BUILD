load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob([
        "*.java",
        "native_sources/*.js",
    ]),
    # TODO(b/202515129): Allow dependencies on native code (like js & iOS) in J2KT.
    generate_kt_readables = False,
    generate_wasm_readables = False,
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

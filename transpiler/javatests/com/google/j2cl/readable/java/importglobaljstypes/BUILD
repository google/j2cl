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
    # @JsType not supported in Kotlin.
    generate_kt_readables = False,
    generate_wasm_imports = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

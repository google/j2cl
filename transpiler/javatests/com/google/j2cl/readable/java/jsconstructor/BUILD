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
    # TODO(b/529376111): Uncomment when implicit constructors in non-public classes are translated
    # as explicit in Kotlin.
    # generate_kt_web_readables = True,
    generate_wasm_externs = True,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

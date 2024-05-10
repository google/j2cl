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
        "*.native.js",
    ]),
    # TODO(b/325056024): Enable when jsenum is supported in the modular pipeline.
    use_modular_pipeline = False,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

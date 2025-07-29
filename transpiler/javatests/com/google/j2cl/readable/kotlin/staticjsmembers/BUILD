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
        "*.kt",
        "native_sources/*.js",
    ]),
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

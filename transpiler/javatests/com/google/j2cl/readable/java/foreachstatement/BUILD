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
    # TODO(b/342159812): Re-enable build tests when kotlin code is valid.
    build_kt_native_readables = False,
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

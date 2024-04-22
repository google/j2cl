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
    # TODO(b/202513859): Add support for experimental_optimize_autovalue in J2KT.
    generate_kt_readables = False,
    deps = [
        "//third_party:auto_value_generates_api-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)

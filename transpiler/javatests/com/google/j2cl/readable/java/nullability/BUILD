load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["**/*.java"]),
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)

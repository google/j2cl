load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

j2cl_library(
    name = "fakes",
    srcs = glob(["fakes/**/*.java"]),
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    j2kt_experimental_webasync_coroutines = True,
    tags = ["no_grok"],
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

# Readable examples for J2KT @WebAsync coroutine lowering.
# WebAsyncExample, Super, JsIgnoreTest, Synchronized, and Lambda mirror the corresponding
# Java-to-Java plugin golden tests in javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/
# so the generated outputs can be compared side-by-side.
readable_example(
    testonly = True,
    srcs = glob(["*.java"]),
    generate_js_readables = False,
    generate_wasm_readables = False,
    j2kt_experimental_webasync_coroutines = True,
    deps = [
        ":fakes",
        "//third_party:j2objc_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

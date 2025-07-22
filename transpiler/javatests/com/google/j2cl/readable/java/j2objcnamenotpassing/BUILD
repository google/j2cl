load("//build_defs:rules.bzl", "j2kt_jvm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    j2kt_new_objc_names = False,
    deps = [
        "//third_party:j2objc_annotations-j2kt-jvm",
        "//third_party:jspecify_annotations-j2kt-jvm",
    ],
)

readable_example(
    srcs = glob([
        "*.java",
    ]),
    # TODO(b/425618126): Problems with @ObjCName and diamond hierarchies.
    build_kt_native_readables = False,
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    generate_js_readables = False,
    generate_wasm_readables = False,
    j2kt_j2objc_interop_enabled = True,
    deps = [
        "//third_party:j2objc_annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

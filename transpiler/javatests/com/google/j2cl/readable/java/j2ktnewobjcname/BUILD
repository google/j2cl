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
        "srcspackage/*.java",
    ]),
    # TODO(b/413285345): Uncomment when it starts building, when Kotlin compiler plugin
    #  generates correct @ObjCName annotations.
    build_kt_readables = False,
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    generate_js_readables = False,
    generate_wasm_readables = False,
    j2kt_j2objc_interop_enabled = True,
    deps = [
        "//third_party:j2objc_annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
        "//transpiler/javatests/com/google/j2cl/readable/java/j2ktnewobjcname/depspackage:depspackage-j2cl",
    ],
)

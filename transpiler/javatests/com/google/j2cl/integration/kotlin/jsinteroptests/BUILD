load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

alias(
    name = "closure_reflect-j2cl",
    actual = "//third_party:closure_reflect",
)

integration_test(
    name = "jsinteroptests",
    srcs = glob([
        "**/*.kt",
        "**/*.js",
    ]) + ["package-info.java"],
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    deps = [
        ":closure_reflect",
        "//third_party:jspecify_annotations",
    ],
)

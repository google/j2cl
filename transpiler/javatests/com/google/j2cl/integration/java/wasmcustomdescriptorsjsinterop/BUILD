load("@rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

# Use a separate js_library for MyNativeType to make sure that native type references in the
# J2Wasm externs are resolved correctly. (b/553008530)
closure_js_library(
    name = "MyNativeType",
    srcs = ["super-wasm/MyNativeType.js"],
)

j2wasm_library(
    name = "wasmcustomdescriptorsjsinterop-j2wasm",
    srcs = glob(
        [
            "super-wasm/*.java",
            "super-wasm/*.js",
        ],
        exclude = ["super-wasm/MyNativeType.js"],
    ),
    # TODO(b/499050043): This is a workaround to avoid errors with `build :all` but must be
    # revisited once we have a final solution for different srcs according to feature set.
    tags = ["manual"],
    deps = [
        ":MyNativeType",
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    srcs = glob(["*.java"]),
    # Features only applicable to Wasm.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    enable_wasm_custom_descriptors_jsinterop = True,
    wasm_entry_points = [
        "wasmcustomdescriptorsjsinterop.Main#entry.*",
        "wasmcustomdescriptorsjsinterop.Main.SomeJsType#staticMethod",
    ],
)

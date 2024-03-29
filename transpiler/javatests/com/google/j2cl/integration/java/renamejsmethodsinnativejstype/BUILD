load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

closure_js_library(
    name = "native_types-j2cl",
    srcs = ["NativeFoo.js"],
)

j2cl_library(
    name = "foo-j2cl",
    # Src's JUST the Foo class.
    srcs = ["Foo.java"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    deps = [
        ":native_types-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

integration_test(
    name = "renamejsmethodsinnativejstype",
    # Src's JUST the Main class and gets Foo as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = [
        "Main.java",
        "Main.native.js",
    ],
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":foo",
        ":native_types",
    ],
)

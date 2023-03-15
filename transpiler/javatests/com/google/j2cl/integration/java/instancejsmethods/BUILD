load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "SuperParent-j2cl",
    srcs = ["SuperParent.java"],
)

j2cl_library(
    name = "MyInterface-j2cl",
    srcs = ["MyInterface.java"],
)

j2cl_library(
    name = "Parent-j2cl",
    srcs = ["Parent.java"],
    deps = [
        ":SuperParent-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "Child-j2cl",
    srcs = ["Child.java"],
    deps = [
        ":MyInterface-j2cl",
        ":Parent-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

integration_test(
    name = "instancejsmethods",
    srcs = [
        "Main.java",
        "Main.native.js",
    ],
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":Child",
        ":MyInterface",
        ":Parent",
        ":SuperParent",
    ],
)

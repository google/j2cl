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
    srcs = ["SuperParent.kt"],
)

j2cl_library(
    name = "MyInterface-j2cl",
    srcs = ["MyInterface.kt"],
)

j2cl_library(
    name = "Parent-j2cl",
    srcs = ["Parent.kt"],
    deps = [
        ":SuperParent-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "Child-j2cl",
    srcs = ["Child.kt"],
    deps = [
        ":MyInterface-j2cl",
        ":Parent-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

integration_test(
    srcs = [
        "Main.kt",
        "instancejsmethods.MainKt.native.js",
    ],
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    deps = [
        ":Child",
        ":MyInterface",
        ":Parent",
        ":SuperParent",
    ],
)

load("//build_defs/internal_do_not_use:j2cl_library.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "otherlib-j2cl",
    srcs = glob(
        ["*.kt"],
        exclude = ["Main.kt"],
    ),
    deps = [
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

integration_test(
    srcs = ["Main.kt"],
    enable_jvm_test = False,  # Utilizes native types/functions.
    deps = [":otherlib"],
)

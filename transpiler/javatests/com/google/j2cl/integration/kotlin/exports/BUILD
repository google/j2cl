load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "Foo",
    srcs = ["Foo.kt"],
)

integration_library(
    name = "Bar",
    srcs = ["Bar.kt"],
)

integration_library(
    name = "ExportLayerOne",
    exports = [
        ":Bar",
        ":Foo",
    ],
)

integration_library(
    name = "ExportLayerTwo",
    exports = [":ExportLayerOne"],
)

integration_test(
    name = "exports",
    srcs = ["Main.kt"],
    deps = [":ExportLayerTwo"],
)

load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "Foo",
    srcs = ["Foo.java"],
)

j2cl_library(
    name = "Bar",
    srcs = ["Bar.java"],
)

j2cl_library(
    name = "ExportLayerOne",
    exports = [
        ":Bar",
        ":Foo",
    ],
)

j2cl_library(
    name = "ExportLayerTwo",
    exports = [":ExportLayerOne"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "exports",
    srcs = ["Main.java"],
    enable_kt = False,
    deps = [":ExportLayerTwo"],
)

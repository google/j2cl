load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

j2cl_library(
    name = "Foo",
    srcs = ["Foo.java"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
)

j2cl_library(
    name = "Bar",
    srcs = ["Bar.java"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
)

j2cl_library(
    name = "ExportLayerOne",
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    exports = [
        ":Bar",
        ":Foo",
    ],
)

j2cl_library(
    name = "ExportLayerTwo",
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    exports = [":ExportLayerOne"],
)

integration_test(
    name = "exports",
    srcs = ["Main.java"],
    enable_kt = False,
    deps = [":ExportLayerTwo"],
)

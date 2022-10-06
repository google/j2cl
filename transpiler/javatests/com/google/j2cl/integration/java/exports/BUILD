load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

java_library(
    name = "Foo",
    srcs = ["Foo.java"],
)

j2cl_library(
    name = "Foo-j2cl",
    srcs = ["Foo.java"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
)

java_library(
    name = "Bar",
    srcs = ["Bar.java"],
)

j2cl_library(
    name = "Bar-j2cl",
    srcs = ["Bar.java"],
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
)

java_library(
    name = "ExportLayerOne",
    exports = [
        ":Bar",
        ":Foo",
    ],
)

j2cl_library(
    name = "ExportLayerOne-j2cl",
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    exports = [
        ":Bar-j2cl",
        ":Foo-j2cl",
    ],
)

java_library(
    name = "ExportLayerTwo",
    exports = [":ExportLayerOne"],
)

j2cl_library(
    name = "ExportLayerTwo-j2cl",
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    exports = [":ExportLayerOne-j2cl"],
)

integration_test(
    name = "exports",
    srcs = ["Main.java"],
    enable_kt = False,
    deps = [":ExportLayerTwo-j2cl"],
)

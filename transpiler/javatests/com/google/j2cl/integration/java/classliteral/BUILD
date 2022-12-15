load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(licenses = ["notice"])

j2wasm_library(
    name = "classliteral-j2wasm",
    srcs = glob(
        ["*.java"],
        exclude = ["TypeExtendsNativeClass.java"],
    ) + glob(["super-wasm/*.java"]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "classliteral",
    srcs = glob(["*.java"]),
    # TODO(b/253494330): Enable this when foo.class.getName() is handled properly for both Java and
    # Javascript.
    enable_jvm_test = False,
    # TODO(b/202428351): Generics support
    enable_kt = False,
)

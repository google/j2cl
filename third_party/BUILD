# Description:
#   Thirdparty dependencies indirection for bazel.

load("//build_defs:rules.bzl", "j2cl_alias", "j2cl_import", "j2cl_library")

package(
    default_visibility = ["//:__subpackages__"],
    licenses = ["notice"],
)

alias(
    name = "asm",
    actual = "@google_bazel_common//third_party/java/asm",
)

alias(
    name = "guava",
    actual = "@google_bazel_common//third_party/java/guava",
)

alias(
    name = "jsr305_annotations",
    actual = "@google_bazel_common//third_party/java/jsr305_annotations",
)

j2cl_import(
    name = "jsr305_annotations-j2cl",
    jar = ":jsr305_annotations",
)

alias(
    name = "j2objc_annotations",
    actual = "@maven//:com_google_j2objc_j2objc_annotations",
)

j2cl_import(
    name = "j2objc_annotations-j2cl",
    jar = ":j2objc_annotations",
)

alias(
    name = "jspecify_annotations",
    actual = "@google_bazel_common//third_party/java/jspecify_annotations",
)

j2cl_import(
    name = "jspecify_annotations-j2cl",
    jar = ":jspecify_annotations",
)

alias(
    name = "jscomp",
    actual = "@io_bazel_rules_closure//closure/compiler",
)

alias(
    name = "args4j",
    actual = "@maven//:args4j_args4j",
)

java_library(
    name = "jdt-core",
    exports = [
        "@maven//:org_eclipse_jdt_org_eclipse_jdt_core",
        "@maven//:org_eclipse_platform_org_eclipse_equinox_common",
    ],
)

alias(
    name = "binaryen",
    actual = "@com_google_binaryen//:wasm-opt",
)

alias(
    name = "auto_value",
    actual = "@google_bazel_common//third_party/java/auto:value",
)

j2cl_import(
    name = "auto_value-j2cl",
    jar = ":auto_value",
)

alias(
    name = "auto_service",
    actual = "@google_bazel_common//third_party/java/auto:service",
)

alias(
    name = "auto_common",
    actual = "@google_bazel_common//third_party/java/auto:common",
)

alias(
    name = "error_prone_annotations",
    actual = "@google_bazel_common//third_party/java/error_prone:annotations",
)

alias(
    name = "gson",
    actual = "@maven//:com_google_code_gson_gson",
)

java_library(
    name = "escapevelocity",
    exports = [
        "@maven//:com_google_escapevelocity_escapevelocity",
    ],
)

java_library(
    name = "junit",
    testonly = 1,
    exports = ["@google_bazel_common//third_party/java/junit"],
)

j2cl_library(
    name = "junit-j2cl",
    testonly = 1,
    exports = ["//:junit"],
)

java_library(
    name = "mockito",
    testonly = 1,
    exports = ["@google_bazel_common//third_party/java/mockito"],
)

java_library(
    name = "truth",
    testonly = 1,
    exports = ["@google_bazel_common//third_party/java/truth"],
)

java_library(
    name = "compile_testing",
    testonly = 1,
    exports = ["@maven//:com_google_testing_compile_compile_testing"],
)

alias(
    name = "runfiles",
    actual = "@bazel_tools//tools/java/runfiles",
)

alias(
    name = "protobuf_util_json",
    actual = "@com_google_protobuf//:protobuf_java_util",
)

alias(
    name = "closure_math_long",
    actual = "@io_bazel_rules_closure//closure/library:long",
)

alias(
    name = "closure_reflect",
    actual = "@io_bazel_rules_closure//closure/library:reflect",
)

alias(
    name = "closure_testcase",
    actual = "@io_bazel_rules_closure//closure/testing/library:testcase",
)

alias(
    name = "closure_testsuite",
    actual = "@io_bazel_rules_closure//closure/testing/library:testsuite",
)

alias(
    name = "jsinterop-annotations",
    actual = "@com_google_jsinterop_annotations-j2cl//:jsinterop-annotations",
)

j2cl_alias(
    name = "jsinterop-annotations-j2cl",
    actual = "@com_google_jsinterop_annotations-j2cl//:jsinterop-annotations-j2cl",
)

alias(
    name = "jsinterop-annotations-j2cl_jar",
    actual = "@com_google_jsinterop_annotations-j2cl//java/jsinterop/annotations:annotations-j2cl_jar",
    visibility = ["//jre/java:__pkg__"],
)

# Reimport the jar file without "neverlink=1" so we can use it as a runtime dependency
java_import(
    name = "kotlin_compiler",
    jars = ["@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar"],
)

# Description:
#   Thirdparty dependencies indirection for bazel.

load("//build_defs:rules.bzl", "j2cl_alias", "j2cl_import", "j2cl_library")

package(
    default_visibility = ["//:__subpackages__"],
    licenses = ["notice"],
)

alias(
    name = "guava",
    actual = "@com_google_guava",
)

alias(
    name = "jsr305_annotations",
    actual = "@com_google_code_findbugs_jsr305",
)

alias(
    name = "j2objc_annotations",
    actual = "@j2objc_annotations",
)

j2cl_import(
    name = "j2objc_annotations-j2cl",
    jar = "@j2objc_annotations//jar",
)

alias(
    name = "jspecify_annotations",
    actual = "@org_jspecify",
)

j2cl_import(
    name = "jspecify_annotations-j2cl",
    jar = "@org_jspecify//jar",
)

alias(
    name = "jscomp",
    actual = "@io_bazel_rules_closure//closure/compiler",
)

alias(
    name = "args4j",
    actual = "@args4j",
)

java_library(
    name = "jdt-core",
    exports = [
        "@org_eclipse_jdt_compiler_apt//jar",
        "@org_eclipse_jdt_content_type//jar",
        "@org_eclipse_jdt_core//jar",
        "@org_eclipse_jdt_equinox_common//jar",
        "@org_eclipse_jdt_equinox_preferences//jar",
        "@org_eclipse_jdt_jobs//jar",
        "@org_eclipse_jdt_osgi//jar",
        "@org_eclipse_jdt_resources//jar",
        "@org_eclipse_jdt_runtime//jar",
        "@org_eclipse_jdt_text//jar",
    ],
)

alias(
    name = "javac",
    actual = "@com_sun_tools_javac//jar",
)

sh_binary(
    name = "binaryen",
    srcs = ["binaryen.sh"],
)

alias(
    name = "auto_value",
    actual = "@com_google_auto_value",
)

j2cl_import(
    name = "auto_value-j2cl",
    jar = "@com_google_auto_value//jar",
)

java_library(
    name = "auto_service",
    exported_plugins = [":auto_service_plugin"],
    exports = ["@com_google_auto_service_annotations//jar"],
)

java_plugin(
    name = "auto_service_plugin",
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = [
        ":auto_common",
        ":guava",
        "@com_google_auto_service//jar",
        "@com_google_auto_service_annotations//jar",
    ],
)

alias(
    name = "auto_common",
    actual = "@com_google_auto_common//jar",
)

alias(
    name = "gson",
    actual = "@com_google_code_gson",
)

java_library(
    name = "escapevelocity",
    exports = [
        "@com_google_escapevelocity//jar",
    ],
)

java_library(
    name = "junit",
    testonly = 1,
    exports = ["@org_junit//jar"],
)

j2cl_library(
    name = "junit-j2cl",
    testonly = 1,
    exports = ["//:junit"],
)

java_library(
    name = "mockito",
    testonly = 1,
    exports = ["@org_mockito//jar"],
)

java_library(
    name = "truth",
    testonly = 1,
    exports = ["@com_google_truth//jar"],
)

java_library(
    name = "compile_testing",
    testonly = 1,
    exports = ["@com_google_testing_compile//jar"],
)

alias(
    name = "protobuf_util_json",
    actual = "@com_google_protobuf//:protobuf_java_util",
)

alias(
    name = "closure_asserts",
    actual = "@com_google_javascript_closure_library//closure/goog/asserts",
)

alias(
    name = "closure_math_long",
    actual = "@com_google_javascript_closure_library//closure/goog/math:long",
)

alias(
    name = "closure_reflect",
    actual = "@com_google_javascript_closure_library//closure/goog/reflect",
)

alias(
    name = "closure_testcase",
    actual = "@com_google_javascript_closure_library//closure/goog/testing:testcase",
)

alias(
    name = "closure_testsuite",
    actual = "@com_google_javascript_closure_library//closure/goog/testing:testsuite",
)

alias(
    name = "gwt-jsinterop-annotations",
    actual = "@com_google_jsinterop_annotations-j2cl//:jsinterop-annotations",
)

j2cl_alias(
    name = "gwt-jsinterop-annotations-j2cl",
    actual = "@com_google_jsinterop_annotations-j2cl//:jsinterop-annotations-j2cl",
)

# Reimport the jar file without "neverlink=1" so we can use it as a runtime dependency
java_import(
    name = "kotlin_compiler",
    jars = ["@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar"],
)

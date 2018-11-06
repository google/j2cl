# Description:
#   Thirdparty dependencies indirection for bazel.

package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "guava",
    exports = ["@com_google_guava"],
)

java_library(
    name = "apache_commons_text",
    exports = ["@org_apache_commons_text//jar"],
    runtime_deps = ["@org_apache_commons_lang3//jar"],
)

java_library(
    name = "jsr305_annotations",
    exports = ["@com_google_code_findbugs_jsr305"],
)

java_library(
    name = "jscomp",
    exports = ["@io_bazel_rules_closure//closure/compiler"],
)

java_library(
    name = "args4j",
    exports = ["@args4j"],
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

java_library(
    name = "auto_value",
    exports = ["@com_google_auto_value"],
)

java_library(
    name = "auto_service",
    exports = ["@com_google_auto_service//jar"],
)

java_library(
    name = "auto_common",
    exports = ["@com_google_auto_common//jar"],
)

java_library(
    name = "gson",
    exports = ["@com_google_code_gson"],
)

java_library(
    name = "jakarta_velocity",
    exports = [
        "@org_apache_velocity//jar",
    ],
    runtime_deps = [
        "@org_apache_commons_collections//jar",
        "@org_apache_commons_lang2//jar",
    ],
)

java_library(
    name = "junit",
    testonly = 1,
    exports = ["@org_junit//jar"],
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

java_library(
    name = "protobuf_json",
    exports = [
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
    ],
)

alias(
    name = "closure_testcase",
    actual = "@io_bazel_rules_closure//closure/library/testing:testcase",
)

alias(
    name = "closure_reflect",
    actual = "@io_bazel_rules_closure//closure/library/reflect",
)

alias(
    name = "closure_math_long",
    actual = "@io_bazel_rules_closure//closure/library/math:long",
)

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

# closure_js_library implicitly includes closure base by default but we still
# need a target since there is existing deps.
closure_js_library(
    name = "closure_base",
    srcs = ["@io_bazel_rules_closure//closure/library:base"],
)

alias(
    name = "gwt_java_emul",
    actual = "@org_gwtproject_gwt//user:java_emul",
)

alias(
    name = "gwt_java_emul_internal",
    actual = "@org_gwtproject_gwt//user:java_emul_internal",
)

alias(
    name = "gwt-javaemul-internal-annotations",
    actual = "@org_gwtproject_gwt//user:gwt-javaemul-internal-annotations",
)

load("//build_defs:rules.bzl", "j2cl_import")

j2cl_import(
    name = "gwt-javaemul-internal-annotations-j2cl",
    jar = "@org_gwtproject_gwt//user:gwt-javaemul-internal-annotations",
)

java_library(
    name = "gwt-jsinterop-annotations",
    exports = ["@com_google_jsinterop_annotations//jar"],
)

j2cl_import(
    name = "gwt-jsinterop-annotations-j2cl",
    jar = "@com_google_jsinterop_annotations//jar",
)

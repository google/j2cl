# Description:
#   Thirdparty dependencies indirection for bazel.

package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "guava",
    exports = ["@com_google_guava//jar"],
)

java_library(
    name = "apache_commons_lang3",
    exports = ["@org_apache_commons_lang3//jar"],
)

java_library(
    name = "jsr305_annotations",
    exports = ["@com_google_jsr305//jar"],
)

java_library(
    name = "jscomp",
    exports = ["@com_google_jscomp//jar"],
)

java_library(
    name = "args4j",
    exports = ["@args4j//jar"],
)

java_library(
    name = "gwt-jsinterop-annotations",
    exports = ["@com_google_jsinterop_annotations//jar"],
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
        "@org_eclipse_jdt_runtime_compatibility//jar",
        "@org_eclipse_jdt_text//jar",
    ],
)

java_plugin(
    name = "auto_annotation_plugin",
    processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
    deps = ["@com_google_auto_value//jar"],
)

java_plugin(
    name = "auto_value_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["@com_google_auto_value//jar"],
)

java_library(
    name = "auto_value",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_value_plugin",
    ],
    exports = ["@com_google_auto_value//jar"],
)

java_library(
    name = "auto_service",
    exports = ["@com_google_auto_service//jar"],
)

java_library(
    name = "auto_common",
    exports = ["@com_google_auto_common//jar],
)

java_library(
    name = "jakarta_velocity",
    exports = ["@org_apache_velocity//jar"],
)

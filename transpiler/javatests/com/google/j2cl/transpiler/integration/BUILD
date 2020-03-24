# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(
    licenses = ["notice"],  # Apache 2.0
)

java_import(
    name = "jre",
    jars = [
        "//jre/java:libjre.jar",
    ],
    srcjar = "//jre/java:libjre-src.jar",
    deps = [
        "//jre/java:jre_bootclasspath",
        "//third_party:gwt-javaemul-internal-annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)

# A java_binary to create a merged jar of all deps of the standard runtime.
java_binary(
    name = "jre_bundle",
    create_executable = 0,
    runtime_deps = [":jre"],
)

java_test(
    name = "CommandLineInvocationTest",
    srcs = ["CommandLineInvocationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":TranspilerTester",
        "//third_party:junit",
    ],
)

java_test(
    name = "SourceMapsIntegrationTest",
    srcs = ["SourceMapsIntegrationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":TranspilerTester",
        "//third_party:junit",
    ],
)

java_test(
    name = "KytheIndexingMetadataTest",
    srcs = ["KytheIndexingMetadataTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":TranspilerTester",
        "//third_party:junit",
    ],
)

java_test(
    name = "JsInteropRestrictionsCheckerTest",
    srcs = ["JsInteropRestrictionsCheckerTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":TranspilerTester",
        "//third_party:guava",
        "//third_party:junit",
    ],
)

# This just a convenient way of zipping since that's what is expected for nativesourcepath.
java_library(
    name = "jre_native",
    resources = ["//jre/java:jre_native_files"],
)

java_test(
    name = "RerunningJ2clTranspilerTest",
    srcs = ["RerunningJ2clTranspilerTest.java"],
    data = [
        ":jre_bundle_deploy.jar",
        ":jre_bundle_deploy-src.jar",
        ":libjre_native.jar",
    ],
    deps = [
        ":TranspilerTester",
        "//third_party:guava",
        "//third_party:junit",
    ],
)

java_library(
    name = "TranspilerTester",
    testonly = 1,
    srcs = [
        "TranspilerTester.java",
    ],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler:commandlinerunner_lib",
    ],
)

config_setting(
    name = "tree_artifact_enabled",
    flag_values = {
        "//:enable_experimental_tree_artifact_mode": "True",
    },
)

filegroup(
    name = "integration_tests_warnings_whitelist",
    srcs = select({
        ":tree_artifact_enabled": ["tree_artifact_integration_tests_warnings_whitelist.txt"],
        "//conditions:default": ["integration_tests_warnings_whitelist.txt"],
    }),
    visibility = [":__subpackages__"],
)

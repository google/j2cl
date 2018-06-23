# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

# Apache2
licenses(["notice"])

# A java_binary to create a merged jar of all deps of the standard runtime.
java_binary(
    name = "jre_bundle",
    create_executable = 0,
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
    runtime_deps = ["//jre/java:jre_java_library"],
)

java_test(
    name = "CommandLineInvocationTest",
    srcs = ["CommandLineInvocationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":TranspilerTester",
        "//third_party/java/junit",
    ],
)

java_test(
    name = "SourceMapsIntegrationTest",
    srcs = ["SourceMapsIntegrationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":TranspilerTester",
        "//third_party/java/junit",
    ],
)

java_test(
    name = "JsInteropRestrictionsCheckerTest",
    srcs = ["JsInteropRestrictionsCheckerTest.java"],
    data = [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":TranspilerTester",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/junit",
    ],
)

java_library(
    name = "TranspilerTester",
    testonly = 1,
    srcs = [
        "TranspilerTester.java",
    ],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler:commandlinerunner_lib",
    ],
)

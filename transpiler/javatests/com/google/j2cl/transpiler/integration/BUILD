# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(default_visibility = ["//:__subpackages__"])

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
    name = "HelpFlagTest",
    srcs = ["HelpFlagTest.java"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "InvalidFlagTest",
    srcs = ["InvalidFlagTest.java"],
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "SyntaxErrorTest",
    srcs = ["SyntaxErrorTest.java"],
    data = glob(["syntaxerror/**/*"]),
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "MissingDepTest",
    srcs = ["MissingDepTest.java"],
    data = glob(["missingdep/**/*"]),
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "SrcjarTest",
    srcs = ["SrcjarTest.java"],
    data = glob([
        "corruptsrcjar/**/*",
        "missingsrcjar/**/*",
    ]),
    deps = [":IntegrationTestCase"],
)

java_test(
    name = "NativeSourcesErrorTest",
    srcs = ["NativeSourcesErrorTest.java"],
    data = glob(["nativesourceserror/**/*"]) + [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":IntegrationTestCase",
        "//transpiler/java/com/google/j2cl/generator",
    ],
)

java_test(
    name = "OutputTest",
    srcs = ["OutputTest.java"],
    data = glob(["output/**/*"]) + [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":IntegrationTestCase",
        "//java/com/google/common/io",
    ],
)

java_test(
    name = "JsInteropRestrictionsCheckerTest",
    srcs = ["JsInteropRestrictionsCheckerTest.java"],
    data = [":jre_bundle_deploy.jar"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    deps = [
        ":IntegrationTestCase",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
    ],
)

java_library(
    name = "IntegrationTestCase",
    testonly = 1,
    srcs = ["IntegrationTestCase.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//java/com/google/devtools/build/runtime:runfiles",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)

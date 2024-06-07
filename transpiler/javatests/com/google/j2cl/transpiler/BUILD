# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_import(
    name = "jre",
    jars = ["//jre/java:libjre.jar"],
    srcjar = "//jre/java:libjre-src.jar",
    tags = [
        "incomplete-deps",
    ],
    deps = ["//jre/java:jre_bootclasspath"],
)

# A java_binary to create a merged jar of all deps of the standard runtime.
java_binary(
    name = "jre_bundle",
    create_executable = 0,
    runtime_deps = [":jre"],
)

java_import(
    name = "jre-j2wasm",
    jars = ["//jre/java:libjre-j2wasm.jar"],
    srcjar = "//jre/java:libjre-j2wasm-src.jar",
    tags = [
        "incomplete-deps",
    ],
    deps = ["//jre/java:jre_bootclasspath-j2wasm"],
)

# A java_binary to create a merged jar of all deps of the standard runtime for Wasm.
java_binary(
    name = "jre_bundle-j2wasm",
    create_executable = 0,
    runtime_deps = [":jre-j2wasm"],
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
        "//third_party:junit",
    ],
)

java_test(
    name = "J2wasmJsInteropRestrictionsCheckerTest",
    srcs = ["J2wasmJsInteropRestrictionsCheckerTest.java"],
    data = [
        ":jre_bundle-j2wasm_deploy.jar",
        ":jre_bundle-j2wasm_deploy-src.jar",
    ],
    deps = [
        ":TranspilerTester",
        "//third_party:junit",
    ],
)

java_test(
    name = "J2ktRestrictionsCheckerTest",
    srcs = ["J2ktRestrictionsCheckerTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":TranspilerTester",
        "//third_party:junit",
    ],
)

java_test(
    name = "J2wasmTranspilerTest",
    srcs = ["J2wasmTranspilerTest.java"],
    data = [
        ":jre_bundle-j2wasm_deploy.jar",
        ":jre_bundle-j2wasm_deploy-src.jar",
    ],
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
        ":ktstdlib_bundle_deploy.jar",
        ":libjre_native.jar",
        "//samples/box2d/src/main/kotlin/idiomatic:libbox2d_kt_library-j2cl-src.jar",
    ],
    runtime_deps = [
        "//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin",
    ],
    deps = [
        ":TranspilerTester",
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

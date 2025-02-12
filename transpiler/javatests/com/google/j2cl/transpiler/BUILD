# Description:
#   Integration tests.
#
# The IntegrationTestCase base class makes it extremely easy to define and
# verify integration tests that need to assert about output std or err logs.
# Other integration tests will want to run transpiled output as JS and
# should make use of j2cl_library macro.

load(
    "//build_defs:rules.bzl",
    "j2wasm_application",
    "j2wasm_library",
)
load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# A java_binary to create a merged jar of all deps of the standard runtime.
java_binary(
    name = "jre_bundle",
    create_executable = 0,
    runtime_deps = ["//jre/java:jre_bootclasspath"],
)

# A java_binary to create a merged jar of all deps of the standard runtime for Kotlin backend.
java_binary(
    name = "jre_bundle-j2kt",
    create_executable = 0,
    runtime_deps = ["//third_party/java/xplat/j2kt:jre_bootclasspath-j2kt"],
)

# A java_binary to create a merged jar of all deps of the standard runtime for Wasm.
java_binary(
    name = "jre_bundle-j2wasm",
    create_executable = 0,
    runtime_deps = ["//jre/java:jre_bootclasspath-j2wasm"],
)

java_test(
    name = "CommandLineInvocationTest",
    srcs = ["CommandLineInvocationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":tester_lib",
        "//third_party:junit",
    ],
)

java_test(
    name = "SourceMapsIntegrationTest",
    srcs = ["SourceMapsIntegrationTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":tester_lib",
        "//third_party:junit",
    ],
)

java_test(
    name = "KytheIndexingMetadataTest",
    srcs = ["KytheIndexingMetadataTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":tester_lib",
        "//third_party:junit",
    ],
)

java_test(
    name = "JsInteropRestrictionsCheckerTest",
    srcs = ["JsInteropRestrictionsCheckerTest.java"],
    data = [":jre_bundle_deploy.jar"],
    deps = [
        ":tester_lib",
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
        ":tester_lib",
        "//third_party:junit",
    ],
)

java_test(
    name = "J2ktRestrictionsCheckerTest",
    srcs = ["J2ktRestrictionsCheckerTest.java"],
    data = [":jre_bundle-j2kt_deploy.jar"],
    deps = [
        ":tester_lib",
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
        ":tester_lib",
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
        "//samples/box2d/src/main/kotlin/idiomatic:libbox2d_library-j2cl-src.jar",
    ],
    jvm_flags = [
        # Configure JVM to provide more consistent performance since we rely on execution times.
        "-XX:+TieredCompilation",
        "-XX:TieredStopAtLevel=2",
        "-XX:MaxGCPauseMillis=100",
    ],
    shard_count = 2,
    deps = [
        ":tester_lib",
        "//third_party:junit",
    ],
)

j2wasm_library(
    name = "sampleapp_lib",
    srcs = ["SampleApp.java"],
)

j2wasm_application(
    name = "sampleapp",
    entry_points = ["com.google.j2cl.transpiler.SampleApp#main"],
    deps = [":sampleapp_lib"],
)

java_test(
    name = "WasmSourceMapTest",
    srcs = ["WasmSourceMapTest.java"],
    data = [
        ":sampleapp_dev",
    ],
    tags = ["j2wasm"],
    deps = [
        ":tester_lib",
        "//third_party:jscomp",
        "//third_party:junit",
        "//third_party/java/truth",
    ],
)

java_library(
    name = "tester_lib",
    testonly = 1,
    srcs = ["TranspilerTester.java"],
    javacopts = ["-Xep:BetaApi:OFF"],
    deps = [
        "//third_party:error_prone_annotations",
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:runfiles",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler:commandlinerunner_lib",
    ],
)

java_library(
    name = "tester_lib_public",
    testonly = 1,
    data = [
        ":jre_bundle_deploy.jar",
    ],
    visibility = ["//benchmarking/java/com/google/j2cl/benchmarks/transpiler:__pkg__"],
    exports = [":tester_lib"],
)

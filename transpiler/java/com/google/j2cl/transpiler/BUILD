load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load(":compiler_java_binary.bzl", "compiler_java_binary")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "transpiler_lib",
    srcs = [
        "J2clTranspiler.java",
        "J2clTranspilerOptions.java",
    ],
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend",
        "//transpiler/java/com/google/j2cl/transpiler/frontend",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/passes",
    ],
)

java_library(
    name = "commandlinerunner_lib",
    srcs = ["J2clCommandLineRunner.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl:__subpackages__"],
    deps = [
        ":transpiler_lib",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/backend",
        "//transpiler/java/com/google/j2cl/transpiler/frontend",
    ],
)

java_binary(
    name = "J2clCommandLineRunner",
    jvm_flags = [
        "-XX:+TieredCompilation",
    ],
    main_class = "com.google.j2cl.transpiler.J2clCommandLineRunner",
    use_launcher = False,
    runtime_deps = [":commandlinerunner_lib"],
)

java_library(
    name = "bazelbuilder_lib",
    srcs = ["BazelJ2clBuilder.java"],
    deps = [
        ":transpiler_lib",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
        "//transpiler/java/com/google/j2cl/transpiler/backend",
        "//transpiler/java/com/google/j2cl/transpiler/frontend",
    ],
)

java_library(
    name = "bazelwasmexportgenerator_lib",
    srcs = ["BazelJ2wasmExportsGenerator.java"],
    deps = [
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        # TODO(b/294284380): Make this independent of the frontend.
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/wasm",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)

java_library(
    name = "bazelj2wasmbundler_lib",
    srcs = ["BazelJ2wasmBundler.java"],
    deps = [
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        # TODO(b/294284380): Make this independent of the frontend.
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
        "//transpiler/java/com/google/j2cl/transpiler/backend/wasm",
        "//transpiler/java/com/google/j2cl/transpiler/backend/wasm:summary_java_proto",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
        "//transpiler/java/com/google/j2cl/transpiler/passes",
    ],
)

compiler_java_binary(
    name = "BazelJ2clBuilder",
    main_class = "com.google.j2cl.transpiler.BazelJ2clBuilder",
    runtime_deps = [":bazelbuilder_lib"],
)

compiler_java_binary(
    name = "BazelJ2wasmExportGenerator",
    main_class = "com.google.j2cl.transpiler.BazelJ2wasmExportsGenerator",
    runtime_deps = [":bazelwasmexportgenerator_lib"],
)

compiler_java_binary(
    name = "BazelJ2wasmBundler",
    extra_jvm_flags = [
        # TODO(b/404243318): Remove this flag once a better solution is found.
        # Limit the heap size to 11GB to avoid OOMs.
        "-Xmx10g",
    ],
    main_class = "com.google.j2cl.transpiler.BazelJ2wasmBundler",
    runtime_deps = [":bazelj2wasmbundler_lib"],
)

config_setting(
    name = "profiling_disabled",
    flag_values = {"//:profiling_filter": "<disabled>"},
)

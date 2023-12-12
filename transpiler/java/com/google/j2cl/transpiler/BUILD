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
        "-Dj2cl.deterministicTimestamps=true",
    ],
    main_class = "com.google.j2cl.transpiler.J2clCommandLineRunner",
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
        "//transpiler/java/com/google/j2cl/transpiler/backend/wasm",
        "//transpiler/java/com/google/j2cl/transpiler/backend/wasm:summary_java_proto",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/jdt",
    ],
)

JVM_FLAGS = [
    "-XX:+TieredCompilation",
    "-Xss2M",
    # Disable bytecode verification to save from class-loading time.
    "-Xverify:none",
    "-Dj2cl.deterministicTimestamps=true",
]

# Like JVM_FLAGS, but using the C2 compiler.
# When modular compilation for Wasm is enabled, this can be removed in favor
# of using the same flags as non-wasm transpilation.
JVM_FLAGS_FOR_LARGE_HEAP = [
    "-Xss2M",
    # Disable bytecode verification to save from class-loading time.
    "-Xverify:none",
    "-Dj2cl.deterministicTimestamps=true",
]

java_binary(
    name = "BazelJ2clBuilder",
    jvm_flags = JVM_FLAGS,
    main_class = "com.google.j2cl.transpiler.BazelJ2clBuilder",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":bazelbuilder_lib"],
)

java_binary(
    name = "BazelJ2clBuilderForLargeHeap",
    jvm_flags = JVM_FLAGS_FOR_LARGE_HEAP,
    main_class = "com.google.j2cl.transpiler.BazelJ2clBuilder",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":bazelbuilder_lib"],
)

java_binary(
    name = "BazelJ2wasmExportGenerator",
    jvm_flags = JVM_FLAGS,
    main_class = "com.google.j2cl.transpiler.BazelJ2wasmExportsGenerator",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":bazelwasmexportgenerator_lib"],
)

java_binary(
    name = "BazelJ2wasmBundler",
    jvm_flags = JVM_FLAGS,
    main_class = "com.google.j2cl.transpiler.BazelJ2wasmBundler",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":bazelj2wasmbundler_lib"],
)

# J2CL binary with Kotlin compiler dependency.
# TODO(b/217287994): Remove after a regular dependency is allowed.
java_binary(
    name = "BazelJ2clBuilderWithKotlinSupport",
    jvm_flags = JVM_FLAGS,
    main_class = "com.google.j2cl.transpiler.BazelJ2clBuilder",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [
        ":bazelbuilder_lib",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/kotlin",
    ],
)

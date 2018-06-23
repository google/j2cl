# Apache2
licenses(["notice"])

java_library(
    name = "transpiler_lib",
    srcs = [
        "J2clTranspiler.java",
        "J2clTranspilerOptions.java",
    ],
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
    ],
)

java_library(
    name = "commandlinerunner_lib",
    srcs = ["J2clCommandLineRunner.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__"],
    deps = [
        ":transpiler_lib",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
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
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__"],
    deps = [
        ":transpiler_lib",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/bazel",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

java_binary(
    name = "BazelJ2clBuilder",
    jvm_flags = [
        "-XX:+TieredCompilation",
        "-Dj2cl.deterministicTimestamps=true",
    ],
    main_class = "com.google.j2cl.transpiler.BazelJ2clBuilder",
    visibility = ["//internal_do_not_use:__pkg__"],
    runtime_deps = [":bazelbuilder_lib"],
)

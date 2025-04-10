"""J2KT JVM allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

J2KT_JVM_ALLOWLIST = allowlists.of_packages([
    "//third_party/java_src/animal_sniffer/...",
    "//third_party/java_src/google_common/current/java/com/google/common/annotations/...",
    "//benchmarking/java/com/google/j2cl/benchmarks/octane/raytrace/...",
    "//build_defs/internal_do_not_use/...",
    "//transpiler/javatests/com/google/j2cl/integration/java/...",
    "//transpiler/javatests/com/google/j2cl/integration/testing/...",
    "//transpiler/javatests/com/google/j2cl/readable/java/...",
    "//jre/javatests/...",
    "//junit/generator/java/com/google/j2cl/junit/apt/...",
    "//junit/generator/java/com/google/j2cl/junit/runtime/...",
    "//junit/generator/javatests/com/google/j2cl/junit/integration/...",
    "//third_party/java_src/j2objc/annotations/...",
    "//third_party/java/animal_sniffer/...",
    "//third_party/java/auto/...",
    "//third_party/java/checker_framework_annotations/...",
    "//third_party/java/error_prone/...",
    "//third_party/java/j2objc/...",
    "//third_party/java/jsr250_annotations/...",
    "//third_party/java/jsr330_inject/...",
    "//third_party/java/junit/...",
])

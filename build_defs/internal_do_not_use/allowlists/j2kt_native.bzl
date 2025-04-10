"""J2KT Native allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

# Packages that j2cl rule will generate j2kt native packages by default. Used to simplify test
# rules.
J2KT_NATIVE_ALLOWLIST = allowlists.of_packages([
    "//java/com/google/thirdparty/publicsuffix/...",
    "//third_party/java/animal_sniffer/...",
    "//third_party/java/auto/...",
    "//third_party/java/checker_framework_annotations/...",
    "//third_party/java/error_prone/...",
    "//third_party/java/findurl/...",
    "//third_party/java/googicu/...",
    "//third_party/java/j2objc/...",
    "//third_party/java/joda_time/...",
    "//third_party/java/jsr250_annotations/...",
    "//third_party/java/jsr330_inject/...",
    "//third_party/java/junit/...",
    "//third_party/java_src/animal_sniffer/...",
    "//third_party/java_src/findurl/...",
    "//third_party/java_src/googicu/...",
    "//build_defs/internal_do_not_use/...",
    "//junit/generator/java/com/google/j2cl/junit/apt/...",
    "//junit/generator/java/com/google/j2cl/junit/runtime/...",
    "//junit/generator/javatests/com/google/j2cl/junit/integration/...",
    "//transpiler/javatests/com/google/j2cl/integration/java/...",
    "//transpiler/javatests/com/google/j2cl/integration/testing/...",
    "//transpiler/javatests/com/google/j2cl/readable/java/...",
    "//third_party/java_src/j2objc/annotations/...",
    "//third_party/java_src/jsr330_inject/...",
])

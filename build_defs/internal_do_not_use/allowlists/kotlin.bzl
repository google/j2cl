"""Kotlin frontend allowlists."""

load(":allowlists.bzl", "allowlists")
load(":j2kt_web.bzl", "J2KT_WEB_ENABLED", "J2KT_WEB_EXPERIMENT_ENABLED")

visibility(["//build_defs/internal_do_not_use/..."])

# Packages that are allowed to have Kotlin inputs to j2cl_library targets.
KOTLIN_ALLOWLIST = allowlists.of_packages([
    "//third_party/bazel_rules/rules_kotlin/...",
    "//...",
    "//third_party/java_src/xplat/j2kt/jre/java/...",
    "//third_party/kotlin/kotlin_kythe_plugin/...",
    "//third_party/kotlin/kmp_stubs/...",
], include = [J2KT_WEB_ENABLED, J2KT_WEB_EXPERIMENT_ENABLED])

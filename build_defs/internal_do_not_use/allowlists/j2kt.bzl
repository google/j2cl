"""Allowlists common to multiple J2KT platforms."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

EXEMPT_FROM_NULLMARKED_ALLOWLIST = allowlists.of_packages([
    "//jre/javatests",
    "//junit/...",
    "//samples/box2d/src/main/java",
    "//third_party/java_src/xplat/j2kt/emulation/android",
    "//third_party/java_src/xplat/j2kt/jre/java",
    "//third_party/java_src/xplat/kmpbench/java",
    # We're permissive of tests not being @NullMarked.
    "//javatests/...",
])

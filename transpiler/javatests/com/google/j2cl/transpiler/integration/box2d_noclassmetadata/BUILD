licenses(["notice"])  # Apache 2.0

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.checkLevel": "NORMAL",
        "jre.classMetadata": "'STRIPPED'",
    },
    defs = [
        # TODO(cromwellian): investigate why JSCompiler doesn't preserve original
        # name before ReplaceStrings sees it.
        # Strip enum names:
        "--replace_strings=module$exports$nativebootstrap$Util$impl.$makeEnumName(?)",
    ],
    tags = ["manual"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/box2d:box2d_j2cl",
    ],
)

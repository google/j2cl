load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.checkLevel": "MINIMAL",
        "jre.classMetadata": "'STRIPPED'",
    },
    defs = [
        # TODO(cromwellian): investigate why JSCompiler doesn't preserve original
        # name before ReplaceStrings sees it.
        # Strip enum names:
        "--replace_strings=module$exports$nativebootstrap$Util$impl.$makeEnumName(?)",
    ],
    enable_kt = False,
    tags = ["manual"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/java/box2d:box2d_j2cl",
    ],
)

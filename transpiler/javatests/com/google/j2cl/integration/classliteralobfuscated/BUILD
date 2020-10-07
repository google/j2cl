load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "classliteralobfuscated",
    srcs = glob(["*.java"]),
    defs = [
        # TODO(cromwellian): investigate why JSCompiler doesn't preserve original name before ReplaceStrings sees it
        # This version is for the :optimized_js target
        "--replace_strings=module$exports$nativebootstrap$Util$impl.$makeClassName(?)",
        # This version is for the :compiled_test target
        "--replace_strings=$Util.$makeClassName(?)",
    ],
    # String replacement does not occur in uncompiled_test
    disable_uncompiled_test = True,
    tags = ["manual"],
)

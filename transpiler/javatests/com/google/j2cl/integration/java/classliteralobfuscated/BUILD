load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

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
    # Class literal obfuscation only works for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    tags = ["manual"],
)

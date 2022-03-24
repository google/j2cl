load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "enumobfuscation",
    srcs = glob(["*.java"]),
    defs = [
        # TODO(cromwellian): investigate why JSCompiler doesn't preserve original name before ReplaceStrings sees it
        # This version is for the :optimized_js target
        "--replace_strings=module$exports$nativebootstrap$Util$impl.$makeEnumName(?):!enumobfuscation/Main$Foo.impl.java.js",
        # This version is for the :compiled_test target
        "--replace_strings=$Util.$makeEnumName(?):!enumobfuscation/Main$Foo.impl.java.js",
    ],
    # String replacement does not occur in uncompiled_test
    disable_uncompiled_test = True,
    # TODO(b/219913874): java.lang.Enum to kotlin.Enum translation
    enable_kt = False,
    # Enum obfuscation is not supported in WASM.
    enable_wasm = False,
    tags = ["manual"],
)

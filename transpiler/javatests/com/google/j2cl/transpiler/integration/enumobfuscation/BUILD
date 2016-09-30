package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
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
)

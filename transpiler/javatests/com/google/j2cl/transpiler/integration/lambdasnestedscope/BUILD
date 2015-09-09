package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/integration_test",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "lambdasnestedscope",
    srcs = glob(["*.java"]),
    # TODO: fix the hidden JS output bug and remove this suppression.
    defs = ["--jscomp_warning=checkTypes"],
)

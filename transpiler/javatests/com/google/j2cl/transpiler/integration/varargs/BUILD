package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/integration_test",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "varargs",
    srcs = glob(["*.java"]),
)

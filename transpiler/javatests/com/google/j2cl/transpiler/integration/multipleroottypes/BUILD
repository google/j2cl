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
    name = "multipleroottypes",
    # Src's JUST the Main class and gets MainRootType as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = ["Main.java"],
    deps = [":MainRootType"],
)

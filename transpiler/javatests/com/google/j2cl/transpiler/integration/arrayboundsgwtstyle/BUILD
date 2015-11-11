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
    name = "arrayboundsgwtstyle",
    srcs = glob(["*.java"]),
    # For now ARRAY_CHECK_BOUNDS_ defaults to off to be GWT compatible, but the
    # integration_test() macro turns it on. This test in particular wants to
    # verify that you can cheat when ARRAY_CHECK_BOUNDS_ is false, thus this
    # provided setting.
    closure_defines = {"ARRAY_CHECK_BOUNDS_": "false"},
)

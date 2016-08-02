package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# To get the GWT optimized size:
# blaze build third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/box2d:optimized_gwt_application
# then unzip the jar and find the js file.

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "box2d",
    srcs = glob(["*.java"]),
    closure_defines = {
        "ASSERTIONS_ENABLED_": "false",
        "jre.classMetadata": "'STRIPPED'",
    },
    enable_gwt = True,
    gwt_deps = [
        "//java/com/google/common/collect:collect-gwt",
        "//third_party/java/jbox2d:jbox2d_gwt",
    ],
    deps = [
        "//third_party/java/jbox2d:jbox2d_j2cl",
        "//jre/java:gwt-jsinterop-annotations",
    ],
)

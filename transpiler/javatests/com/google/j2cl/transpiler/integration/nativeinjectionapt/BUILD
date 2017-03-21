package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "nativeinjectionapt",
    srcs = glob(["*.java"]),
    js_depinfos = ["handrolled.depinfo"],
    plugins = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt/apt"],
    deps = [
        "//jre/java:gwt-jsinterop-annotations",
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt/apt:annotation",
    ],
)

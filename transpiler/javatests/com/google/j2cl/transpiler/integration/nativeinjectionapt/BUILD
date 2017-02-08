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
    plugins = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt/apt"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/nativeinjectionapt/apt:annotation",
    ],
)

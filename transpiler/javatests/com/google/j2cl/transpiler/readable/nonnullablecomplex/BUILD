package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "nonnullablecomplex",
    srcs = ["Main.java"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/nonnullablecomplex/sub:NonNullableExample",
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/nonnullablecomplex/sub:NullableExample",
    ],
)

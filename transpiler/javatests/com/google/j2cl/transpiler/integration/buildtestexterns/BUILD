package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

filegroup(
    name = "foo_externs",
    srcs = ["foo_externs.js"],
)

integration_test(
    name = "buildtestexterns",
    srcs = glob(["*.java"]),
    test_externs_list = [
        ":foo_externs",
    ],
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

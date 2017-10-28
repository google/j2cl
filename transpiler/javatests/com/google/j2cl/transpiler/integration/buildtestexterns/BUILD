# Apache2
licenses(["notice"])

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
    closure_defines = {
        "jre.checks.checkLevel": "'NORMAL'",
        "jre.checkedMode": "'ENABLED'",
    },
    test_externs_list = [":foo_externs"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)

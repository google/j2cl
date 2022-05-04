load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

j2cl_library(
    name = "MainRootType",
    # Src's JUST the MainRootType class.
    srcs = ["MainRootType.java"],
)

integration_test(
    name = "multipleroottypes",
    # Src's JUST the Main class and gets MainRootType as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = ["Main.java"],
    deps = [":MainRootType"],
)

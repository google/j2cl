load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "otherlib",
    srcs = [
        "ClassWithCompanion.kt",
        "InlineFunctionsInAnotherLibrary.kt",
        "OnlyReferencedInOtherLib.kt",
    ],
)

readable_example(
    srcs = glob(
        ["*.kt"],
        exclude = [
            "ClassWithCompanion.kt",
            "InlineFunctionsInAnotherLibrary.kt",
            "OnlyReferencedInOtherLib.kt",
        ],
    ),
    deps = [":otherlib"],
)

load(
    "//build_defs:rules.bzl",
    "j2cl_library",
)
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "ThirdPartyLib",
    srcs = ["ThirdPartyLibClass.kt"],
)

readable_example(
    srcs = [
        "Companion.kt",
        "Other.kt",
    ],
    deps = [
        ":ThirdPartyLib",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

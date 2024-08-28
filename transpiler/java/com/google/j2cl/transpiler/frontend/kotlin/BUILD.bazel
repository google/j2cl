package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

# Temporary placeholder.
java_library(
    name = "kotlin",
    srcs = glob(["*.java"]),
    deps = [
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)

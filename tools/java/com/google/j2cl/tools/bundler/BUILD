# Description:
#  Bundler for j2wasm output.

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_binary(
    name = "J2wasmBundler_worker",
    main_class = "com.google.j2cl.tools.bundler.BazelJ2wasmBundler",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":bundler"],
)

java_library(
    name = "bundler",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
    ],
)

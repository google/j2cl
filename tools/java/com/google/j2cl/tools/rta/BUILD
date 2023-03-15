# Description:
#  Implementation of RTA algorithm used to do fast method pruning.

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

proto_library(
    name = "code_removal_info_proto",
    srcs = ["code_removal_info.proto"],
)

java_proto_library(
    name = "code_removal_info_java_proto",
    visibility = [
        "//tools/java/com/google/j2cl/tools/minifier:__pkg__",
        "//tools/javatests/com/google/j2cl/tools:__subpackages__",
    ],
    deps = [":code_removal_info_proto"],
)

java_binary(
    name = "J2clRta_worker",
    main_class = "com.google.j2cl.tools.rta.BazelJ2clRta",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":rta"],
)

java_library(
    name = "rta",
    srcs = glob(["*.java"]),
    deps = [
        ":code_removal_info_java_proto",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
        "//transpiler/java/com/google/j2cl/transpiler/backend/libraryinfo",
    ],
)

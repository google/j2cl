# Description:
#  Implementation of RTA algorithm used to do fast method pruning.

# Apache2
licenses(["notice"])

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

# TODO(b/114042401): Consider to add a Worker
java_binary(
    name = "J2clRtaRunner",
    main_class = "com.google.j2cl.tools.rta.J2clRta",
    visibility = [
        "//java/com/google/caribou/ui/pinto/modules:__subpackages__",
        "//:__subpackages__",
    ],
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
        "//third_party:protobuf_json",
        "//transpiler/java/com/google/j2cl/libraryinfo",
    ],
)

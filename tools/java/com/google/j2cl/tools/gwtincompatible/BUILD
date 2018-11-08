# Description:
#   This builds the gwtincompatible stripping tool.  This tool looks for @GwtIncompatible
#   annotations on source code elements and comments them out so that they do not get parsed by
#   J2CL.

licenses(["notice"])  # Apache 2.0

java_library(
    name = "gwtincompatible_lib",
    srcs = glob(
        ["*.java"],
        exclude = ["StripperWorker.java"],
    ),
    visibility = ["//tools/javatests/com/google/j2cl/tools/gwtincompatible:__pkg__"],
    deps = [
        "//third_party:args4j",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

java_binary(
    name = "GwtIncompatibleStripper",
    jvm_flags = [
        "-Dj2cl.deterministicTimestamps=true",
    ],
    main_class = "com.google.j2cl.tools.gwtincompatible.Stripper",
    runtime_deps = [":gwtincompatible_lib"],
)

java_library(
    name = "gwtincompatible_worker_lib",
    srcs = ["StripperWorker.java"],
    deps = [
        ":gwtincompatible_lib",
        "//transpiler/java/com/google/j2cl/bazel",
    ],
)

java_binary(
    name = "GwtIncompatibleStripper_worker",
    jvm_flags = [
        "-Dj2cl.deterministicTimestamps=true",
    ],
    main_class = "com.google.j2cl.tools.gwtincompatible.StripperWorker",
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
    runtime_deps = [":gwtincompatible_worker_lib"],
)

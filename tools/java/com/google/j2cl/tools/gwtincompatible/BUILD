# Description:
#   This builds the gwtincompatible stripping tool.  This tool looks for @GwtIncompatible
#   annotations on source code elements and comments them out so that they do not get parsed by
#   J2CL.

# Apache2
licenses(["notice"])

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

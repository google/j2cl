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
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party:jdt-core",
        "//third_party/java/args4j",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

java_binary(
    name = "GwtIncompatibleStripper",
    main_class = "com.google.j2cl.tools.gwtincompatible.Stripper",
    runtime_deps = [":gwtincompatible_lib"],
)

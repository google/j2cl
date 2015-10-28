package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/build_def/j2cl_java_library",
    "j2cl_java_library",
)

# Only transpiles the example but not compiles by JSCompiler.
# It will not compile by JSCompiler because the java.lang.String is provided twice.
# TODO: Creates verification target
j2cl_java_library(
    name = "javalangstring",
    srcs = glob(["**/*.java"]),
    javacopts = [
        "-source 8",
        "-target 8",
    ],
)

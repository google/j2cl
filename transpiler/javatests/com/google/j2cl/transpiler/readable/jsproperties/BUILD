package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "jsproperties",
    srcs = glob(["*.java"]),
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

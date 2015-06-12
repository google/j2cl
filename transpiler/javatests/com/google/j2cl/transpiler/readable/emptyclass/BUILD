package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example",
    "readable_example",
)

# Creates verification target
# blaze build :emptyclass_binary
readable_example(
    name = "emptyclass",
    srcs = glob(["*.java"]),
)

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "forwarder_java_library",
    exports = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsforwarding/foo:foo_java_library"],
)

js_library(
    name = "forwarder",
    deps = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsforwarding/foo"],
)

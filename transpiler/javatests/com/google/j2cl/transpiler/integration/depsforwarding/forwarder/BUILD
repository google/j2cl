package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "forwarder_java_library",
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
    exports = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsforwarding/foo:foo_java_library"],
)

js_library(
    name = "forwarder",
    deps_mgmt = "legacy",
    exports = [
        "//transpiler/javatests/com/google/j2cl/transpiler/integration/depsforwarding/foo",
    ],
)

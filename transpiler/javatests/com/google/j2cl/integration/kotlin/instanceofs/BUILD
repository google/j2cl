load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_test(
    srcs = glob(["*.kt"]),
    defs = [
        # TODO(b/550539875): uncomment once we no longer generate property invalidation.
        # "--properties_that_must_disambiguate=shouldDisambiguate"
    ],
)

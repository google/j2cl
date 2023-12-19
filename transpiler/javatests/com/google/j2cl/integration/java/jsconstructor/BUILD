load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "jsconstructor",
    srcs = glob(["*.java"]),
    # TODO: b/316920865 - Enable when JsConstructor is properly propagated.
    enable_kt_web = False,
)

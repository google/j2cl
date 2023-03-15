load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "arrayofboxedtype",
    srcs = glob(["*.java"]),
    # TODO(b/227554485): Kotlin Native does not throw ArrayStoreException when
    # storing wrong type of object.
    enable_kt_native = False,
)

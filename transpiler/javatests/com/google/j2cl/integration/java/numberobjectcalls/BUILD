load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "numberobjectcalls",
    srcs = glob(["*.java"]),
    # TODO(b/242816223): Class.getName() gives different results on JVM and
    # Native for primitive and mapped types.
    enable_kt_native = False,
)

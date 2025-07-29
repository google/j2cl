load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "narrowingdoubletofloat",
    srcs = glob(["*.kt"]),
    # Code running on the JVM honors float-double precision differences
    enable_jvm_test = False,
)

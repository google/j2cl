load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    srcs = [
        "ActualFoo.kt",
        "Main.kt",
    ],
    # TODO(b/249165777): Enable jvm test when the Xmultiplatform feature is no longer experimental.
    enable_jvm_test = False,
    kt_common_srcs = ["Foo.kt"],
)

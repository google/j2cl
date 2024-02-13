load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "assertremoved",
    srcs = glob(["*.java"]),
    defs = [
        # Turn off asserts since it is what this test is testing.
        "--remove_j2cl_asserts=true",
    ],
    disable_uncompiled_test = True,
    disable_wasm_dev_test = True,
    # TODO(b/249154012): Turn this on when we can disable assertions on the JVM.
    enable_jvm_test = False,
    enable_kt = False,
    # TODO(b/b/325055794): Removal of asserts not yet implemented in the modular pipeline
    enable_wasm_modular = False,
    tags = ["manual"],
)

package(
    licenses = ["notice"],  # Apache 2.0
)

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "assertsimple",
    srcs = glob(["*.java"]),
    defs = [
        # Turn on asserts since it is what this test is testing.
        "--remove_j2cl_asserts=false",
    ],
    tags = ["manual"],
)

load("@com_google_j2cl//build_defs:rules.bzl", "j2wasm_test")

package(
    default_applicable_licenses = ["@com_google_j2cl//:j2cl_license"],
    licenses = ["notice"],
)

j2wasm_test(
    name = "HelloWorldTest",
    srcs = glob(["*.java"]),
    deps = [
        "@com_google_j2cl//:junit-j2wasm",
        "@com_google_j2cl//samples/wasm/src/main/java/com/google/j2cl/samples/wasm:helloworld",
    ],
)

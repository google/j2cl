load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_test")

package(
    default_applicable_licenses = ["@com_google_j2cl//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_test(
    name = "HelloWorldTest",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/j2cl/samples/helloworldlib",
        "@com_google_j2cl//:junit",
    ],
)

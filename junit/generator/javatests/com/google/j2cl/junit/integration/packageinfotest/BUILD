load("//build_defs:rules.bzl", "j2cl_library", "j2cl_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "packageinfo",
    srcs = ["package-info.java"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

j2cl_library(
    name = "lib",
    testonly = 1,
    srcs = ["FileWithPackageInfoTest.java"],
    deps = [
        ":packageinfo",
        "//third_party:junit-j2cl",
    ],
)

# Test that j2cl_test works fine w.r.t package-info strict deps (b/78013961).
j2cl_test(
    name = "FileWithPackageInfoTest",
    runtime_deps = [":lib"],
)

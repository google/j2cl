# Apache2
licenses(["notice"])

load("//build_def:j2cl_test.bzl", "j2cl_test")

# Test that j2cl_test works fine w.r.t package-info strict deps (b/78013961).
j2cl_test(
    name = "FileWithPackageInfoTest",
    runtime_deps = [":lib"],
)

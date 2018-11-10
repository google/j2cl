workspace(name = "com_google_j2cl")

load("//build_defs:repository.bzl", "setup_j2cl_workspace")

setup_j2cl_workspace()

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_repositories")

closure_repositories(
    omit_com_google_protobuf = True,
)

workspace(name = "com_google_j2cl")

load("//build_defs:repository.bzl", "load_j2cl_repo_deps")
load_j2cl_repo_deps()

load("//build_defs:workspace.bzl", "setup_j2cl_workspace")
setup_j2cl_workspace()

# Needed to run unit tests in Chrome.
load("@io_bazel_rules_closure//closure:defs.bzl", "setup_web_test_repositories")
setup_web_test_repositories(
    chromium = True,
)

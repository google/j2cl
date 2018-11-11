workspace(name = "com_google_j2cl")

http_archive(
    name = "io_bazel_rules_closure",
    strip_prefix = "rules_closure-master",
    url = "https://github.com/bazelbuild/rules_closure/archive/master.zip",
)

load("//build_defs:rules.bzl", "setup_j2cl_workspace")

setup_j2cl_workspace()

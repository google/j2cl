"""Bazel rule for loading external repository deps for J2CL."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def load_j2cl_repo_deps():
    _github_repo(
        name = "io_bazel_rules_closure",
        repo = "bazelbuild/rules_closure",
        tag = "6531168e3107b00769efe5a5148faf503a66d9ed",
        sha256 = "273a14a4682725ce77f1b9fbb0b5f85cb9a4170da58dcf54e049fd9d66929482",
    )

    _github_repo(
        name = "bazel_skylib",
        repo = "bazelbuild/bazel-skylib",
        tag = "0.7.0",
        sha256 = "bce240a0749dfc52fab20dce400b4d5cf7c28b239d64f8fd1762b3c9470121d8",
    )

    # Add other closure repo deps that need to loaded beforehand.
    # A differnt alternative would be to let rules_closure load them but then
    # we would need to take setup_j2cl_workspace out of rules.bzl and go with a
    # different setup route - which will be a breaking change.
    # This alternative keeps backward compatibility until we figure out how to
    # proceed.

    _github_repo(
        name = "rules_java",
        repo = "bazelbuild/rules_java",
        tag = "981f06c3d2bd10225e85209904090eb7b5fb26bd",
        sha256 = "7979ece89e82546b0dcd1dff7538c34b5a6ebc9148971106f0e3705444f00665",
    )

    _github_repo(
        name = "rules_proto",
        repo = "bazelbuild/rules_proto",
        tag = "97d8af4dc474595af3900dd85cb3a29ad28cc313",
        sha256 = "e4fe70af52135d2ee592a07f916e6e1fc7c94cf8786c15e8c0d0f08b1fe5ea16",
    )

    _github_repo(
        name = "rules_python",
        repo = "bazelbuild/rules_python",
        tag = "4b84ad270387a7c439ebdccfd530e2339601ef27",
        sha256 = "7e6e20edb31da85be4236b51c6f705e7717b9e7bb8d33fe7d713d3cd270257df",
    )

def _github_repo(name, repo, tag, sha256 = None):
    if native.existing_rule(name):
        return

    _, project_name = repo.split("/")
    http_archive(
        name = name,
        strip_prefix = "%s-%s" % (project_name, tag),
        url = "https://github.com/%s/archive/%s.zip" % (repo, tag),
        sha256 = sha256,
    )

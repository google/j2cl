"""Bazel rule for loading external repository deps for J2CL."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def load_j2cl_repo_deps():
    _github_repo(
        name = "io_bazel_rules_closure",
        repo = "bazelbuild/rules_closure",
        tag = "321e678c64656f9dfc39e978a0a017d2930eb7e8",
        sha256 = "ef43294decfa865e9a46de3389d9115c782003e48a8b7dfbcc05faa233ca082f",
    )

    _github_repo(
        name = "bazel_skylib",
        repo = "bazelbuild/bazel-skylib",
        tag = "1.0.2",
        sha256 = "64ad2728ccdd2044216e4cec7815918b7bb3bb28c95b7e9d951f9d4eccb07625",
    )

    _github_repo(
        name = "io_bazel_rules_kotlin",
        repo = "bazelbuild/rules_kotlin",
        tag = "legacy-1.3.0-rc3",
        sha256 = "54678552125753d9fc0a37736d140f1d2e69778d3e52cf454df41a913b964ede"
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

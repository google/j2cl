"""Tests for allowlists.bzl."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load(":allowlists.bzl", "allowlists")

def allowlists_test_suite(name):
    unittest.suite(
        name,
        _allowlist_of_packages_test,
        _allowlist_of_targets_test,
        _allowlist_with_excludes_test,
    )

def _assert_accepts(env, allowlist, package):
    asserts.true(
        env,
        allowlist.accepts(package),
        "Allowlist should accept package %s, but does not." % package,
    )

def _assert_rejects(env, allowlist, package):
    asserts.false(
        env,
        allowlist.accepts(package),
        "Allowlist should reject package %s, but does not." % package,
    )

def _allowlist_of_packages_test_impl(ctx):
    env = unittest.begin(ctx)
    allowlist = allowlists.of_packages(["//foo/...", "//bar"])

    _assert_accepts(env, allowlist, "//foo")
    _assert_accepts(env, allowlist, "//foo:buzz")
    _assert_accepts(env, allowlist, "//foo/bar")
    _assert_accepts(env, allowlist, "//foo/bar/buzz")
    _assert_accepts(env, allowlist, "//bar")
    _assert_accepts(env, allowlist, "//bar:buzz")

    _assert_rejects(env, allowlist, "//baz")
    _assert_rejects(env, allowlist, "//bar/baz")
    _assert_rejects(env, allowlist, "//bar/baz:buzz")

    return unittest.end(env)

_allowlist_of_packages_test = unittest.make(_allowlist_of_packages_test_impl)

def _allowlist_of_targets_test_impl(ctx):
    env = unittest.begin(ctx)
    allowlist = allowlists.of_targets(["//foo:bar", "//foo"])

    _assert_accepts(env, allowlist, "//foo")
    _assert_accepts(env, allowlist, "//foo:foo")
    _assert_accepts(env, allowlist, "//foo:bar")

    _assert_rejects(env, allowlist, "//foo/bar")
    _assert_rejects(env, allowlist, "//foo/bar:buzz")
    _assert_rejects(env, allowlist, "//foo:buzz")
    _assert_rejects(env, allowlist, "//bar")
    _assert_rejects(env, allowlist, "//bar:buzz")

    return unittest.end(env)

_allowlist_of_targets_test = unittest.make(_allowlist_of_targets_test_impl)

def _allowlist_with_excludes_test_impl(ctx):
    env = unittest.begin(ctx)
    allowlist = allowlists.of_packages(
        ["//foo/..."],
        exclude = [
            allowlists.of_packages(["//foo"]),
            allowlists.of_packages(["//bar"]),
        ],
    )

    _assert_accepts(env, allowlist, "//foo/bar")
    _assert_accepts(env, allowlist, "//foo/bar:buzz")

    _assert_rejects(env, allowlist, "//foo")
    _assert_rejects(env, allowlist, "//foo:bar")
    _assert_rejects(env, allowlist, "//bar")
    _assert_rejects(env, allowlist, "//bar:buzz")
    _assert_rejects(env, allowlist, "//baz")

    return unittest.end(env)

_allowlist_with_excludes_test = unittest.make(_allowlist_with_excludes_test_impl)

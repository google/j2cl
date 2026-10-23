"""Check explicit, configurable, and legacy J2CL test compilation modes."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//build_defs:rules.bzl", "j2cl_test")

def _compilation_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(env, ctx.attr.expected, ctx.attr.actual)
    return unittest.end(env)

_compilation_test = unittest.make(
    _compilation_test_impl,
    attrs = {"actual": attr.string(), "expected": attr.string()},
)

def test_compilation_suite(name):
    browsers = ["@rules_webtesting//browsers:chromium-local"]
    selected = select({":optimized": "ADVANCED", "//conditions:default": "BUNDLE"})
    cases = [
        ("advanced", {"browsers": browsers, "compilation_level": "ADVANCED"}, "ADVANCED"),
        ("simple", {"browsers": browsers, "compilation_level": "SIMPLE"}, "SIMPLE"),
        ("bundle", {"browsers": browsers, "compilation_level": "BUNDLE"}, "BUNDLE"),
        ("selected", {"browsers": browsers, "compilation_level": selected}, selected),
        ("legacy_browser", {"browsers": browsers}, "BUNDLE"),
        ("legacy_compiled", {"browsers": browsers, "compile": True}, "ADVANCED"),
        ("legacy_no_browser", {}, "ADVANCED"),
    ]
    tests = []
    for suffix, args, expected in cases:
        subject = name + "_" + suffix
        j2cl_test(
            name = subject,
            runtime_deps = ["//:junit"],
            test_class = "example.ExampleTest",
            tags = ["manual"],
            **args
        )
        test = subject + "_test"
        _compilation_test(
            name = test,
            actual = native.existing_rule(subject + "_bin")["compilation_level"],
            expected = expected,
        )
        tests.append(test)
    native.test_suite(name = name, tests = tests)

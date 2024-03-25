"""integration_test build macro

A build macro that turns Java files into optimized and unoptimized test targets.

The set of Java files must have a Main class with a main() function.


Example usage:

# Creates targets
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "foobar",
    srcs = glob(["*.java"]),
)

"""

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_test")
load("//build_defs:rules.bzl", "J2CL_TEST_DEFS", "j2cl_library")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package", "to_parallel_target")

JAVAC_FLAGS = [
    "-XepDisableAllChecks",
]

# TODO(b/119637659): abstract common behaviour and merge with the internal version.
def integration_test(
        name,
        srcs,
        deps = [],
        defs = [],
        enable_nullability = False,
        main_class = None,
        closure_defines = dict(),
        suppress = [],
        tags = [],
        **kwargs):
    """Macro that turns Java files into integration test targets.

    deps are Labels of j2cl_library() rules. NOT labels of
    java_library() rules.
    """

    # figure out the current location
    java_package = get_java_package(native.package_name())

    if not main_class:
        main_class = java_package + ".Main"

    define_flags = ["--define=%s=%s" % (k, v) for (k, v) in closure_defines.items()]

    defs = defs + define_flags

    java_test_runner = """
      @jsinterop.annotations.JsType(namespace = jsinterop.annotations.JsPackage.GLOBAL)
      public class TestRunner {
        public static void testMain() throws Throwable {
          %s.main((String []) null);
        }
      }
    """ % (main_class)
    _genfile("TestRunner.java", java_test_runner, tags)

    j2cl_library(
        name = "%s-TestRunner" % name,
        srcs = ["TestRunner.java"],
        generate_build_test = False,
        deps = [
            ":%s-j2cl" % name,
            "//third_party:jsinterop-annotations-j2cl",
        ],
        javacopts = JAVAC_FLAGS,
        tags = tags,
    )

    integration_library(
        name = name,
        srcs = srcs,
        deps = deps,
        tags = tags,
        js_suppress = suppress,
        enable_nullability = enable_nullability,
    )

    # blaze test :uncompiled_test
    # blaze test :compiled_test

    test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();

      var testSuite = goog.require('goog.testing.testSuite');
      var TestRunner = goog.require('TestRunner');

      testSuite({ testJ2cl: TestRunner.testMain });
    """
    _genfile("TestHarness_test.js", test_harness, tags)

    closure_js_test(
        name = "compiled_test",
        srcs = ["TestHarness_test.js"],
        deps = [
            ":%s-j2cl" % name,
            ":%s-TestRunner" % name,
            "@com_google_javascript_closure_library//closure/goog/testing:testsuite",
        ],
        # closure_js_test test infra is flaky so avoid noise in builds.
        flaky = True,
        defs = J2CL_TEST_DEFS + defs,
        suppress = suppress,
        testonly = True,
        tags = tags,
        entry_points = ["gen.test.Harness"],
    )

def integration_library(name, srcs = [], deps = [], exports = [], enable_nullability = False, **kwargs):
    default_deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:jsinterop-annotations",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing",
    ]

    if srcs:
        deps = default_deps + deps

    # For open-source we only generate J2CL targets for now.
    j2cl_library(
        name = _to_j2cl_name(name),
        srcs = srcs,
        deps = [to_parallel_target(d, _to_j2cl_name) for d in deps],
        exports = [to_parallel_target(e, _to_j2cl_name) for e in exports],
        javacopts = JAVAC_FLAGS,
        generate_build_test = False,
        experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = enable_nullability,
        **kwargs
    )

def _to_j2cl_name(name):
    return name + "-j2cl"

def _genfile(name, str, tags):
    native.genrule(
        name = name.replace(".", "_"),
        outs = [name],
        cmd = "echo \"%s\" > $@" % str,
        tags = tags,
    )

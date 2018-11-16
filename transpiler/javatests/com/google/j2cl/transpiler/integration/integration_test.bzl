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
load("//build_defs:rules.bzl", "J2CL_OPTIMIZED_DEFS", "J2CL_TEST_DEFS", "j2cl_library")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package")

JAVAC_FLAGS = [
    "-XepDisableAllChecks",
]

# TODO(b/119637659): abstract common behaviour and merge with the internal version.
def integration_test(
        name,
        srcs,
        deps = [],
        defs = [],
        native_srcs = [],
        js_deps = [],
        main_class = None,
        enable_gwt = False,
        gwt_deps = [],
        closure_defines = dict(),
        disable_uncompiled_test = False,
        disable_compiled_test = False,
        suppress = [],
        j2cl_library_tags = [],
        tags = [],
        plugins = []):
    """Macro that turns Java files into integration test targets.

    deps are Labels of j2cl_library() rules. NOT labels of
    java_library() rules.
    """

    # figure out the current location
    java_package = get_java_package(native.package_name())

    if not main_class:
        main_class = java_package + ".Main"

    optimized_extra_defs = [
        # Turn on asserts since the integration tests rely on them.
        # TODO: Enable once the option is made available.
        #        "--remove_j2cl_asserts=false",
        # Avoid 'use strict' noise.
        #        "--emit_use_strict=false",
        # Polyfill re-write is disabled so that size tracking only focuses on
        # size issues that are actionable outside of JSCompiler or are expected
        # to eventually be addressed inside of JSCompiler.
        # TODO: Phantomjs needs polyfills for some features used in tests.
        #"--rewrite_polyfills=false",
        # Cuts optimize time nearly in half and the optimization leaks that it
        # previously hid no longer exist.
        "--closure_entry_point=gen.opt.Harness",
        # Since integration tests are used for optimized size tracking, set
        # behavior to the mode with the smallest output size which is what we
        # expect to be used for customer application production releases.
        # TODO: Enable once the remove_j2cl_asserts option is made available.
        #       "--define=jre.checks.checkLevel=MINIMAL",
    ]

    define_flags = ["--define=%s=%s" % (k, v) for (k, v) in closure_defines.items()]

    defs = defs + define_flags

    j2cl_library(
        name = name,
        srcs = srcs,
        generate_build_test = False,
        deps = deps,
        javacopts = JAVAC_FLAGS,
        _js_deps = js_deps,
        native_srcs = native_srcs,
        plugins = plugins,
        tags = tags + j2cl_library_tags,
        js_suppress = suppress,
    )

    # blaze test :uncompiled_test
    # blaze test :compiled_test

    test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();

      var testSuite = goog.require('goog.testing.testSuite');
      var Main = goog.require('%s');
      testSuite({
        test_Main: function() {
          return Main.m_main__arrayOf_java_lang_String([]);
        }
      });
  """ % (main_class)
    _genfile("TestHarness_test.js", test_harness, tags)

    closure_js_test(
        name = "compiled_test",
        srcs = ["TestHarness_test.js"],
        deps = [
            ":" + name,
            "@io_bazel_rules_closure//closure/library:testing",
        ],
        defs = J2CL_TEST_DEFS + optimized_extra_defs + defs,
        suppress = suppress,
        testonly = True,
        tags = tags,
        entry_points = ["gen.test.Harness"],
    )

def _genfile(name, str, tags):
    native.genrule(
        name = name.replace(".", "_"),
        outs = [name],
        cmd = "echo \"%s\" > $@" % str,
        tags = tags,
    )

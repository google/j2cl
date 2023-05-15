"""This module contains j2cl_js_provider helpers."""

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    "CLOSURE_JS_TOOLCHAIN_ATTRS",
    "closure_js_binary",
    "closure_js_test",
    "create_closure_js_library",
    "web_library",
)

def create_js_lib_struct(j2cl_info, extra_providers = []):
    return struct(
        providers = [j2cl_info] + extra_providers,
        closure_js_library = j2cl_info._private_.js_info.closure_js_library,
        exports = j2cl_info._private_.js_info.exports,
    )

def j2cl_js_provider(ctx, srcs = [], deps = [], exports = [], artifact_suffix = ""):
    """ Creates a js provider from provided sources, deps and exports. """

    if artifact_suffix == "j2wasm":
        # TODO(b/282247604): wire the modular wasm pipeline.
        return struct()

    default_j2cl_suppresses = [
        "analyzerChecks",
        "underscore",
        "superfluousSuppress",
        "JSC_UNKNOWN_EXPR_TYPE",
    ]
    suppresses = default_j2cl_suppresses + getattr(ctx.attr, "js_suppress", [])

    js = create_closure_js_library(
        ctx,
        srcs,
        deps,
        exports,
        suppresses,
        convention = "GOOGLE",
    )

    return struct(
        closure_js_library = js.closure_js_library,
        exports = js.exports,
    )

def js_devserver(
        name,
        entry_point_defs,
        deps,
        dev_resources,
        dev_server_host,
        dev_server_port,
        **kwargs):
    """Creates a development server target."""

    closure_js_binary(
        name = name,
        compilation_level = "BUNDLE",
        defs = entry_point_defs,
        deps = deps,
        # For J2CL it is in impractical to embed all source into sourcemap since
        # it bloats sourcemaps as well as it slows down bundling.
        nodefs = ["--source_map_include_content"],
        **kwargs
    )

    web_library(
        name = "%s_server" % name,
        srcs = dev_resources,
        host = dev_server_host,
        port = dev_server_port,
        path = "/",
        tags = [
            "ibazel_live_reload",  # Enable ibazel reload server.
            "ibazel_notify_changes",  # Do not to restart the server on changes.
        ],
    )

js_binary = closure_js_binary

J2CL_JS_TOOLCHAIN_ATTRS = CLOSURE_JS_TOOLCHAIN_ATTRS

J2CL_JS_ATTRS = {
    "js_suppress": attr.string_list(),
}

JS_PROVIDER_NAME = "closure_js_library"

J2CL_OPTIMIZED_DEFS = [
    "--define=goog.DEBUG=false",
]

# TODO(phpham): Consider adding default test defs here if any.
J2CL_TEST_DEFS = []

# buildifier: disable=function-docstring-args
def j2cl_web_test(
        name,
        deps,
        test_class,
        tags,
        **args):  # @unused
    # TODO(b/259118921): support multiple testsuites.
    fail_multiple_testsuites = """
        FAIL: j2cl_test currently supports testing with a single testsuite only.
        IF YOU HAVE MULTIPLE TESTSUITES, WE DO NOT KNOW IF ALL OF YOUR TESTS PASS OR NOT!
    """

    fail_suiteclass = """
        FAIL: j2cl_test currently doesn't support testing with @RunWith(Suite.class) format.
        Please directly provide the tests that has @RunWith(JUnit4.class).
    """

    testsuite_file_name = name + "_test.js"

    # unzip generated_suite.js.zip and take 1 testsuite js file
    # fail if multiple testsuites or suiteclasses are provided
    out_zip = ":%s_generated_suite.js.zip" % name
    native.genrule(
        name = "gen" + name + "_test.js",
        srcs = [out_zip],
        outs = [
            testsuite_file_name,
        ],
        cmd = "\n".join([
            "unzip -q -o $(locations %s) *.js -d zip_out/" % out_zip,
            "cd zip_out/",
            "mkdir -p ../$(RULEDIR)",
            "if [ $$(find . -name *.js | wc -l) -ne 1 ]; then",
            "  echo \"%s\"" % fail_multiple_testsuites,
            "  exit 1",
            "fi",
            "testsuite=$$(find . -name %s.js)" % name,
            "if [ -z \"$$testsuite\" ]; then",
            "  echo \"%s\"" % fail_suiteclass,
            "  exit 1",
            "fi",
            "mv \"$$testsuite\" ../$@;",
        ]),
        testonly = 1,
    )

    closure_js_test(
        name = name,
        srcs = [":%s" % testsuite_file_name],
        deps = deps,
        testonly = 1,
        entry_points = ["javatests." + test_class + "_AdapterSuite"],
        tags = tags,
    )

"""This module contains j2cl_js_provider helpers."""

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    "CLOSURE_JS_TOOLCHAIN_ATTRS",
    "closure_js_binary",
    "closure_js_test",
    #    "create_closure_js_library",
    "web_library",
)
load(
    "@io_bazel_rules_closure//closure/private:defs.bzl",
    "ClosureJsLibraryInfo",
)
load(
    "@io_bazel_rules_closure//closure/compiler:closure_js_library.bzl",
    "create_closure_js_library",
)
load(":provider.bzl", "J2clInfo")

def create_js_lib_struct(j2cl_info, extra_providers = []):
    print("the extra providers are" + str(extra_providers))
    return [j2cl_info, j2cl_info._private_.js_info] + extra_providers

def create_wasm_js_lib_struct(js_info, extra_providers = []):
    return extra_providers + [js_info]

def j2cl_js_provider(ctx, srcs = [], deps = [], exports = [], artifact_suffix = ""):
    """ Creates a js provider from provided sources, deps and exports. """

    default_j2cl_suppresses = [
        "analyzerChecks",
        "underscore",
        "superfluousSuppress",
        "JSC_UNKNOWN_EXPR_TYPE",
    ]
    suppresses = default_j2cl_suppresses + getattr(ctx.attr, "js_suppress", [])

    for dep in deps:
        print("tffffffffff* " + str(type(dep)) + ", "+ str(dep[ClosureJsLibraryInfo]) + ",,,,"+str(dep[J2clInfo] if J2clInfo in dep else dep)+"the srcs are" + str(srcs))

    js = create_closure_js_library(
        ctx,
        srcs,
        deps,
        exports,
        suppresses,
        convention = "GOOGLE",
        artifact_suffix = artifact_suffix,
    )
    print("the type of jsinfor in jscommon is" + str(type(js)))
    print("the inf jsinfor in jscommon is" + str(js))
    print("the js[1] in jscommon is" + str(js[1]))
    return js[1]

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

JS_PROVIDER_NAME = ClosureJsLibraryInfo

J2CL_OPTIMIZED_DEFS = [
    "--define=goog.DEBUG=false",
]

# TODO(phpham): Consider adding default test defs here if any.
J2CL_TEST_DEFS = []

# buildifier: disable=function-docstring-args
def j2cl_web_test(
        name,
        src,
        deps,
        browsers,
        data,
        test_class,
        tags,
        default_browser = None,
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
    native.genrule(
        name = "gen" + name + "_test.js",
        srcs = [src],
        outs = [
            testsuite_file_name,
        ],
        cmd = "\n".join([
            "unzip -q -o $(locations %s) *.js -d zip_out/" % src,
            "cd zip_out/",
            "mkdir -p ../$(RULEDIR)",
            "if [ $$(find . -name *.js | wc -l) -ne 1 ]; then",
            "  echo \"%s\"" % fail_multiple_testsuites,
            "  exit 1",
            "fi",
            "testsuite=$$(find . -name *.js)",
            "if [ -z \"$$testsuite\" ]; then",
            "  echo \"%s\"" % fail_suiteclass,
            "  exit 1",
            "fi",
            "mv \"$$testsuite\" ../$@;",
        ]),
        testonly = 1,
    )

    if default_browser and not browsers:
        browsers = [default_browser]

    closure_js_test(
        name = name,
        srcs = [":%s" % testsuite_file_name],
        deps = deps,
        browsers = browsers,
        data = data,
        testonly = 1,
        entry_points = ["javatests." + test_class + "_AdapterSuite"],
        tags = tags,
        lenient = True,
    )

"""This module contains j2cl_js_provider helpers."""

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    "CLOSURE_JS_TOOLCHAIN_ATTRS",
    "closure_js_binary",
    "closure_js_library",
    "create_closure_js_library",
    "web_library",
)

def j2cl_js_provider(ctx, srcs = [], deps = [], exports = []):
    """ Creates a js provider from provided sources, deps and exports. """

    default_j2cl_suppresses = [
        "analyzerChecks",
        "JSC_UNKNOWN_EXPR_TYPE",
        "JSC_STRICT_INEXISTENT_PROPERTY",
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

    return {
        "closure_js_library": js.closure_js_library,
        "exports": js.exports,
    }

def js_devserver(
        name,
        entry_point_defs,
        deps,
        dev_resources,
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
        path = "/",
        tags = [
            "ibazel_live_reload",  # Enable ibazel reload server.
            "ibazel_notify_changes",  # Do not to restart the server on changes.
        ],
    )

def simple_js_lib(**kwargs):
    closure_js_library(no_closure_library = True, **kwargs)

js_binary = closure_js_binary

J2CL_JS_TOOLCHAIN_ATTRS = CLOSURE_JS_TOOLCHAIN_ATTRS

J2CL_JS_ATTRS = {
    "js_suppress": attr.string_list(),
}

JS_PROVIDER_NAME = "closure_js_library"

J2CL_OPTIMIZED_DEFS = [
    "--define=goog.DEBUG=false",
]

# Place holder until we implement unit testing support for open-source.
J2CL_TEST_DEFS = []

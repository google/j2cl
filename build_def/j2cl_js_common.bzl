"""This module contains j2cl_js_provider helpers."""

load("//tools/build_defs/js_toolchain:def.bzl", "JS_TOOLCHAIN_ATTRIBUTE")

def j2cl_js_provider(ctx, srcs = [], deps = [], exports = []):
    """ Creates a js provider from provided sources, deps and exports. """

    return {
        "js": js_common.provider(
            ctx,
            srcs = srcs,
            deps = [d.js for d in deps],
            exports = [e.js for e in exports],
            deps_mgmt = ctx.attr.js_deps_mgmt,
            strict_deps_already_checked = True,
        ),
    }

J2CL_JS_ATTRS = dict(JS_TOOLCHAIN_ATTRIBUTE, **{
    "js_deps_mgmt": attr.string(default = "closure"),
})

JS_PROVIDER_NAME = "js"

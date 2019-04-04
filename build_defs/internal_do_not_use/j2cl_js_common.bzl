"""This module contains j2cl_js_provider helpers."""

load("@io_bazel_rules_closure//closure:defs.bzl", "CLOSURE_JS_TOOLCHAIN_ATTRS", "create_closure_js_library")

def j2cl_js_provider(ctx, srcs = [], deps = [], exports = []):
  """ Creates a js provider from provided sources, deps and exports. """

  default_j2cl_suppresses = [
      "analyzerChecks",
      "JSC_UNKNOWN_EXPR_TYPE",
      "JSC_STRICT_INEXISTENT_PROPERTY",
  ]
  suppresses = default_j2cl_suppresses + getattr(ctx.attr, "js_suppress", [])

  js = create_closure_js_library(
      ctx, srcs, deps, exports, suppresses, convention="GOOGLE")

  return {
      "closure_js_library": js.closure_js_library,
      "exports": js.exports,
  }

J2CL_JS_TOOLCHAIN_ATTRS = CLOSURE_JS_TOOLCHAIN_ATTRS

J2CL_JS_ATTRS = {
    "js_suppress": attr.string_list(),
}

JS_PROVIDER_NAME = "closure_js_library"

J2CL_OPTIMIZED_DEFS = [
    "--define=goog.DEBUG=false",
]

# Place holder until we implement unut testing support for open-source.
J2CL_TEST_DEFS = []

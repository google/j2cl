"""Consumer views using public providers only."""

load("@rules_java//java:defs.bzl", "JavaInfo")

def _java_outputs_impl(ctx):
    return [DefaultInfo(files = depset(ctx.attr.library[JavaInfo].runtime_output_jars))]

java_outputs = rule(
    implementation = _java_outputs_impl,
    attrs = {"library": attr.label(providers = [JavaInfo])},
)

def _javascript_output_impl(ctx):
    return [DefaultInfo(files = depset([
        file
        for file in ctx.attr.library[DefaultInfo].files.to_list()
        if file.is_directory
    ]))]

javascript_output = rule(
    implementation = _javascript_output_impl,
    attrs = {"library": attr.label()},
)

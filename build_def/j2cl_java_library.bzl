"""Common utilities for creating J2CL targets and providers."""

load("//build_def:j2cl_transpile.bzl", "J2CL_TRANSPILE_ATTRS", "j2cl_transpile")
load("//build_def:j2cl_js_common.bzl", "J2CL_JS_ATTRS", "j2cl_js_provider")

# Constructor for the Bazel provider for J2CL.
_J2clInfo = provider(fields = ["_J2clJavaInfo"])

def _impl_j2cl_library(ctx):
    java_srcs = ctx.files.srcs
    js_srcs = ctx.files.native_srcs + ctx.files.js_srcs

    java_provider = _java_compile(ctx, java_srcs)

    js_zip = j2cl_transpile(ctx, java_provider, js_srcs)
    js_deps = [d.js for d in ctx.attr.deps]
    js_exports = [e.js for e in ctx.attr.exports]
    js_outputs = [js_zip] if java_srcs else []

    # This is a workaround to b/35847804 to make sure the zip ends up in the runfiles.
    js_runfiles = _collect_runfiles(ctx, js_outputs, ctx.attr.deps + ctx.attr.exports)

    # Write an empty .jslib output (work around b/38349075 and maybe others).
    ctx.actions.write(ctx.outputs.dummy_jslib, "")

    return struct(
        js = j2cl_js_provider(ctx, srcs = js_outputs, deps = js_deps, exports = js_exports),
        providers = [
            DefaultInfo(
                files = depset(js_outputs + [ctx.outputs.jar, ctx.outputs.dummy_jslib]),
                runfiles = js_runfiles,
            ),
            _J2clInfo(_J2clJavaInfo = java_provider),
        ],
    )

def _collect_runfiles(ctx, files, deps):
    transitive_runfiles = [d[DefaultInfo].default_runfiles.files for d in deps]
    return ctx.runfiles(
        files = files,
        transitive_files = depset(transitive = transitive_runfiles),
    )

def _java_compile(ctx, java_srcs):
    stripped_java_srcs = [_strip_gwt_incompatible(ctx, java_srcs)] if java_srcs else []
    java_deps = [d[_J2clInfo]._J2clJavaInfo for d in ctx.attr.deps if _J2clInfo in d]
    java_exports = [d[_J2clInfo]._J2clJavaInfo for d in ctx.attr.exports if _J2clInfo in d]
    plugins = [p[JavaInfo] for p in ctx.attr.plugins]
    exported_plugins = [p[JavaInfo] for p in ctx.attr.exported_plugins]

    return java_common.compile(
        ctx,
        source_files = ctx.files.srcs_hack,
        source_jars = stripped_java_srcs,
        deps = java_deps,
        exports = java_exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        output = ctx.outputs.jar,
        java_toolchain = ctx.attr._java_toolchain,
        host_javabase = ctx.attr._host_javabase,
        javac_opts = java_common.default_javac_opts(ctx, java_toolchain_attr = "_java_toolchain"),
    )

def _strip_gwt_incompatible(ctx, java_srcs):
    output_file = ctx.actions.declare_file(ctx.label.name + "_stripped-src.jar")

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add("-d", output_file)
    args.add_all(ctx.files.srcs)

    ctx.actions.run(
        progress_message = "Stripping @GwtIncompatible",
        inputs = java_srcs,
        outputs = [output_file],
        executable = ctx.executable._stripper,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "GwtIncompatibleStripper",
    )

    return output_file

_J2CL_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = True),
    "native_srcs": attr.label_list(allow_files = [".native.js", ".zip"]),
    "js_srcs": attr.label_list(allow_files = [".js", ".zip"]),
    "srcs_hack": attr.label_list(allow_files = True),
    "deps": attr.label_list(providers = ["js"]),
    "exports": attr.label_list(providers = ["js"]),
    "plugins": attr.label_list(providers = [JavaInfo]),
    "exported_plugins": attr.label_list(providers = [JavaInfo]),
    "javacopts": attr.string_list(),
    "licenses": attr.license(),
    "_java_toolchain": attr.label(
        default = Label("//tools/jdk:toolchain"),
    ),
    "_host_javabase": attr.label(
        default = Label("//tools/jdk:current_host_java_runtime"),
        cfg = "host",
    ),
    "_stripper": attr.label(
        default = Label("//internal_do_not_use:GwtIncompatibleStripper"),
        cfg = "host",
        executable = True,
    ),
}
_J2CL_LIB_ATTRS.update(J2CL_TRANSPILE_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_JS_ATTRS)

j2cl_library = rule(
    implementation = _impl_j2cl_library,
    attrs = _J2CL_LIB_ATTRS,
    fragments = ["java", "js"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
        "zip_file": "%{name}.js.zip",
        "dummy_jslib": "%{name}.jslib",
    },
)

def _impl_java_import(ctx):
    return struct(
        js = j2cl_js_provider(ctx),
        providers = [_J2clInfo(_J2clJavaInfo = ctx.attr.jar[JavaInfo])],
    )

# helper rule to convert a Java target to a J2CL target.
j2cl_java_import = rule(
    implementation = _impl_java_import,
    attrs = dict(J2CL_JS_ATTRS, **{
        "jar": attr.label(providers = [JavaInfo]),
        "licenses": attr.license(),
    }),
    fragments = ["java", "js"],
)

"""J2CL library rules."""

load(":j2cl_common.bzl", "J2CL_TOOLCHAIN_ATTRS", "J2clInfo", "j2cl_common")
load(":j2cl_js_common.bzl", "J2CL_JS_ATTRS", "JS_PROVIDER_NAME", "j2cl_js_provider")

def _impl_j2cl_library(ctx):
    j2cl_provider = j2cl_common.compile(
        ctx,
        srcs = ctx.files.srcs,
        deps = _j2cl_or_js_providers_of(ctx.attr.deps),
        exports = _j2cl_or_js_providers_of(ctx.attr.exports),
        plugins = _javaplugin_providers_of(ctx.attr.plugins),
        exported_plugins = _javaplugin_providers_of(ctx.attr.exported_plugins),
        output_jar = ctx.outputs.jar,
        javac_opts = ctx.attr.javacopts,
        internal_transpiler_flags = {
            k: getattr(ctx.attr, k)
            for k in _J2CL_INTERNAL_LIB_ATTRS.keys()
        },
        generate_kythe_action = ctx.attr.generate_kythe_action,
    )

    output_js = j2cl_provider._private_.output_js
    output_js_array = [output_js] if output_js else []

    return j2cl_common.create_js_lib_struct(
        j2cl_info = j2cl_provider,
        extra_providers = [
            DefaultInfo(
                files = depset(output_js_array + [ctx.outputs.jar]),
                # TODO(goktug): Remove after b/35847804 is fixed.
                runfiles = _collect_runfiles(ctx, output_js_array, ctx.attr.deps + ctx.attr.exports),
            ),
        ],
    )

def _j2cl_or_js_providers_of(deps):
    return [_j2cl_or_js_provider_of(d) for d in deps]

def _j2cl_or_js_provider_of(dep):
    return dep[J2clInfo] if J2clInfo in dep else dep

def _java_providers_of(deps):
    return [d[JavaInfo] for d in deps]

def _javaplugin_providers_of(deps):
    plugin_provider = getattr(java_common, "JavaPluginInfo") if hasattr(java_common, "JavaPluginInfo") else JavaInfo
    return [d[plugin_provider] for d in deps]

def _collect_runfiles(ctx, files, deps):
    transitive_runfiles = [d[DefaultInfo].default_runfiles.files for d in deps]
    return ctx.runfiles(
        files = files,
        transitive_files = depset(transitive = transitive_runfiles),
    )

_J2CL_INTERNAL_LIB_ATTRS = {
    "readable_source_maps": attr.bool(default = False),
    "readable_library_info": attr.bool(default = False),
    "experimental_optimize_autovalue": attr.bool(default = False),
    "experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage": attr.bool(default = False),
}

_J2CL_LIB_ATTRS = {
    # TODO(goktug): Try to limit this further.
    "srcs": attr.label_list(allow_files = [".java", ".js", ".srcjar", ".jar", ".zip"]),
    "deps": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "exports": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "javacopts": attr.string_list(),
    "generate_kythe_action": attr.bool(default = False),
}
_J2CL_LIB_ATTRS.update(_J2CL_INTERNAL_LIB_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_TOOLCHAIN_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_JS_ATTRS)

j2cl_library = rule(
    implementation = _impl_j2cl_library,
    attrs = _J2CL_LIB_ATTRS,
    fragments = ["java", "js"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
        "jszip": "%{name}.js.zip",
    },
)

def _impl_java_import(ctx):
    return j2cl_common.create_js_lib_struct(
        J2clInfo(
            _private_ = struct(
                java_info = ctx.attr.jar[JavaInfo],
                js_info = j2cl_js_provider(ctx),
                library_info = [],
            ),
            _is_j2cl_provider = 1,
        ),
    )

_J2CL_IMPORT_ATTRS = {
    "jar": attr.label(providers = [JavaInfo]),
}
_J2CL_IMPORT_ATTRS.update(J2CL_TOOLCHAIN_ATTRS)

# helper rule to convert a Java target to a J2CL target.
j2cl_java_import = rule(
    implementation = _impl_java_import,
    attrs = _J2CL_IMPORT_ATTRS,
    fragments = ["java", "js"],
)

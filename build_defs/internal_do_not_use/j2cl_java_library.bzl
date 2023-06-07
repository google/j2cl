"""J2CL library rules."""

load(":provider.bzl", "J2clInfo")
load(":j2cl_common.bzl", "J2CL_TOOLCHAIN_ATTRS", "j2cl_common")
load(":j2cl_js_common.bzl", "J2CL_JS_ATTRS", "JS_PROVIDER_NAME", "j2cl_js_provider")

def _impl_j2cl_library(ctx):
    extra_javacopts = ["-Adagger.fastInit=enabled"]
    if ctx.attr.optimize_autovalue:
        extra_javacopts.append("-Acom.google.auto.value.OmitIdentifiers")

    # Restrict the usage of kotlincopts to internal-only callers.
    # TODO(b/240682589): Setup a way for users to safely pass in flags.
    if (len(ctx.attr.kotlincopts) and not ctx.label.package.startswith("")):
        fail("kotlincopts can only be internally set by J2CL")

    j2cl_provider = j2cl_common.compile(
        ctx,
        srcs = ctx.files.srcs,
        kt_common_srcs = ctx.files.kt_common_srcs,
        deps = _j2cl_or_js_providers_of(ctx.attr.deps),
        exports = _j2cl_or_js_providers_of(ctx.attr.exports),
        plugins = _javaplugin_providers_of(ctx.attr.plugins),
        exported_plugins = _javaplugin_providers_of(ctx.attr.exported_plugins),
        output_jar = ctx.outputs.jar,
        javac_opts = extra_javacopts + ctx.attr.javacopts,
        kotlincopts = ctx.attr.kotlincopts,
        internal_transpiler_flags = {
            k: getattr(ctx.attr, k)
            for k in _J2CL_INTERNAL_LIB_ATTRS.keys()
        },
    )

    output_js = j2cl_provider._private_.output_js
    output = [output_js, ctx.outputs.jar] if output_js else [ctx.outputs.jar]

    return j2cl_common.create_js_lib_struct(
        j2cl_info = j2cl_provider,
        extra_providers = [DefaultInfo(files = depset(output))],
    )

def _j2cl_or_js_providers_of(deps):
    return [_j2cl_or_js_provider_of(d) for d in deps]

def _j2cl_or_js_provider_of(dep):
    return dep[J2clInfo] if J2clInfo in dep else dep

def _javaplugin_providers_of(deps):
    plugin_provider = getattr(java_common, "JavaPluginInfo") if hasattr(java_common, "JavaPluginInfo") else JavaInfo
    return [d[plugin_provider] for d in deps]

_J2CL_INTERNAL_LIB_ATTRS = {
    "readable_source_maps": attr.bool(default = False),
    "readable_library_info": attr.bool(default = False),
    "optimize_autovalue": attr.bool(default = True),
    "experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage": attr.bool(default = False),
}

_J2CL_LIB_ATTRS = {
    # TODO(goktug): Try to limit this further.
    "srcs": attr.label_list(allow_files = [".java", ".kt", ".js", ".srcjar", ".jar", ".zip"]),
    "kt_common_srcs": attr.label_list(allow_files = [".kt"]),
    "deps": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "exports": attr.label_list(providers = [JS_PROVIDER_NAME]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "exec"),
    "javacopts": attr.string_list(),
    "kotlincopts": attr.string_list(),
    #  TODO(b/217287994): Remove the ability to do transpiler override.
    "j2cl_transpiler_override": attr.label(default = None, cfg = "exec", executable = True),
}
_J2CL_LIB_ATTRS.update(_J2CL_INTERNAL_LIB_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_TOOLCHAIN_ATTRS)
_J2CL_LIB_ATTRS.update(J2CL_JS_ATTRS)

j2cl_library = rule(
    implementation = _impl_j2cl_library,
    attrs = _J2CL_LIB_ATTRS,
    fragments = ["java", "js"],
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
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

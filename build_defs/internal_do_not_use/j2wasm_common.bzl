"""Common utilities for creating J2WASM targets and providers."""

load("@bazel_skylib//lib:structs.bzl", "structs")
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")
load(":j2cl_common.bzl", "J2CL_TOOLCHAIN_ATTRS", "j2cl_common")
load(":provider.bzl", "J2wasmInfo")

def _compile(
        ctx,
        name,
        srcs = [],
        deps = [],
        exports = [],
        plugins = [],
        exported_plugins = [],
        output_jar = None,
        javac_opts = [],
        artifact_suffix = ""):
    internal_transpiler_flags = {}
    if ctx.attr._feature_set[BuildSettingInfo].value == J2WASM_FEATURE_SET.CUSTOM_DESCRIPTORS:
        internal_transpiler_flags["experimentalEnableWasmCustomDescriptors"] = True

    j2cl_provider = j2cl_common.compile(
        ctx = ctx,
        srcs = srcs,
        deps = deps,
        exports = exports,
        plugins = plugins,
        exported_plugins = exported_plugins,
        backend = "WASM_MODULAR",
        output_jar = output_jar,
        javac_opts = javac_opts + DEFAULT_J2WASM_JAVAC_OPTS,
        artifact_suffix = artifact_suffix,
        internal_transpiler_flags = internal_transpiler_flags,
    )

    return _create_j2wasm_provider(
        j2cl_provider,
        deps + exports,
    )

def _create_j2wasm_provider(j2cl_provider, deps):
    j2wasm_deps = [d for d in deps if hasattr(d, "_is_j2cl_provider")]

    # The output_js could be "None" if there are no sources.
    modular_output = [j2cl_provider._private_.output_js] if j2cl_provider._private_.output_js else []
    return J2wasmInfo(
        _private_ = struct(
            java_info = j2cl_provider._private_.java_info,
            js_info = j2cl_provider._private_.js_info,
            j2cl_info = j2cl_provider,
            transitive_modules = depset(
                modular_output,
                transitive = [
                    d._private_.transitive_modules
                    for d in j2wasm_deps
                ],
                order = "postorder",
            ),
        ),
        _is_j2cl_provider = 1,
    )

def _to_j2wasm_name(name):
    """Convert a label name used in j2cl to be used in j2wasm"""
    if name.endswith("j2cl_proto"):
        name = name[:-10]
        return "%sj2wasm_proto" % name
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2wasm" % name

j2wasm_common = struct(
    compile = _compile,
    to_j2wasm_name = _to_j2wasm_name,
)

DEFAULT_J2WASM_JAVAC_OPTS = [
    # Preserve the private fields in turbine compilation. In order to create wasm structs for
    # Java classes, all the fields from the super classes need to be seen even if compiling
    # in a different library.
    "-XDturbine.emitPrivateFields",
    # Disable analysis for thread safety since it does not expect to see
    # private fields from dependencies.
    "-Xep:ThreadSafe:OFF",
]

J2WASM_TOOLCHAIN_ATTRS = {}
J2WASM_TOOLCHAIN_ATTRS.update(J2CL_TOOLCHAIN_ATTRS)
J2WASM_TOOLCHAIN_ATTRS.update({
    "_j2cl_java_toolchain": attr.label(
        default = Label("//jre/java:j2wasm_java_toolchain"),
    ),
    "_feature_set": attr.label(
        providers = [BuildSettingInfo],
        default = "//build_defs/internal_do_not_use:j2wasm_feature_set",
    ),
})

J2WASM_FEATURE_SET = struct(
    DEFAULT = "",
    CUSTOM_DESCRIPTORS = "custom_descriptors",
)

J2WASM_FEATURE_SET_VALUES = structs.to_dict(J2WASM_FEATURE_SET).values()

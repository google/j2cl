"""j2kt_library build macro

Takes Java source, translates it into Kotlin.
This is an experimental tool and should not be used.
"""

load(":j2cl_common.bzl", "J2CL_TOOLCHAIN_ATTRS")
load("//build_defs/internal_do_not_use:provider.bzl", "J2clInfo", "J2ktInfo")
load(
    "//build_defs/internal_do_not_use:j2cl_common.bzl",
    "j2cl_common",
)

J2KT_LIB_ATTRS = {
    "srcs": attr.label_list(allow_files = [".java", ".srcjar", ".jar"]),
    "deps": attr.label_list(providers = [J2ktInfo]),
    "exports": attr.label_list(providers = [J2ktInfo]),
    "plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "exported_plugins": attr.label_list(allow_rules = ["java_plugin", "java_library"], cfg = "host"),
    "javacopts": attr.string_list(),
    # TODO(dpo): remove when we don't need temporary hack for the jre
    "_temporary_jre": attr.label(default = Label("//jre/java:jre")),
}

J2KT_LIB_ATTRS.update(J2CL_TOOLCHAIN_ATTRS)

def _from_j2kt_library_to_java_provider(deps):
    return [d[J2ktInfo]._private_.java_info for d in deps]

def _impl_j2kt_library_rule(ctx):
    all_java_deps = _from_j2kt_library_to_java_provider(ctx.attr.deps)

    # TODO(dpo): This is a hack to pass the jre to java  compilation. Once
    # java to kt transpilation is mature enough, we should create
    # a j2kt_library for the jre and pass it as normal dependency.
    all_java_deps.append(ctx.attr._temporary_jre[J2clInfo]._private_.java_info)

    java_provider = j2cl_common.java_compile(
        ctx = ctx,
        name = ctx.attr.name,
        srcs = ctx.files.srcs,
        deps = _from_j2kt_library_to_java_provider(ctx.attr.deps),
        exports = _from_j2kt_library_to_java_provider(ctx.attr.exports),
        plugins = [p[JavaInfo] for p in ctx.attr.plugins],
        exported_plugins = [p[JavaInfo] for p in ctx.attr.exported_plugins],
        output_jar = ctx.outputs.jar,
        javac_opts = ctx.attr.javacopts,
        mnemonic = "J2kt",
    )

    all_srcs = java_provider.source_jars
    classpath = depset(
        java_provider.compilation_info.boot_classpath,
        transitive = [java_provider.compilation_info.compilation_classpath],
    )
    transpile_out = ctx.outputs.ktzip

    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", transpile_out)
    args.add("-experimentalBackend", "KOTLIN")

    args.add_all(all_srcs)

    ctx.actions.run(
        progress_message = "Transpiling to Kotlin %s" % ctx.label,
        inputs = depset(all_srcs, transitive = [classpath]),
        outputs = [transpile_out],
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2kt",
    )

    # TODO(dpo): we need to return a provider used by kotlin blaze rule, so
    # these rules can depend directly in any j2kt_library
    return [J2ktInfo(
        _private_ = struct(
            java_info = java_provider,
        ),
    )]

j2kt_library = rule(
    implementation = _impl_j2kt_library_rule,
    attrs = J2KT_LIB_ATTRS,
    fragments = ["java"],
    outputs = {
        "jar": "lib%{name}.jar",
        "srcjar": "lib%{name}-src.jar",
        "ktzip": "%{name}.kt.zip",
    },
)

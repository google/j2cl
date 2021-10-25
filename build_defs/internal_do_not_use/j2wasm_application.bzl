"""j2wasm_application build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":provider.bzl", "J2wasmInfo")

def _impl_j2wasm_application(ctx):
    deps = ctx.attr.deps + [ctx.attr._jre]
    srcs = _get_transitive_srcs(deps)
    classpath = _get_transitive_classpath(deps)

    runfiles = []
    outputs = []

    transpile_out = ctx.actions.declare_directory(ctx.label.name + "_out")
    args = ctx.actions.args()
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")
    args.add_joined("-classpath", classpath, join_with = ctx.configuration.host_path_separator)
    args.add("-output", transpile_out.path)
    args.add("-experimentalBackend", "WASM")
    args.add_all(ctx.attr.entry_points, before_each = "-experimentalGenerateWasmExport")
    args.add_all(ctx.attr.defines, before_each = "-experimentalDefineForWasm")
    args.add_all(ctx.attr.transpiler_args)

    args.add_all(srcs)

    enable_wasm_checks = ctx.var.get("j2cl_wasm_checks", None) == "1"

    ctx.actions.run(
        progress_message = "Transpiling to WASM %s" % ctx.label,
        inputs = depset(transitive = [srcs, classpath]),
        outputs = [transpile_out],
        executable = ctx.executable._j2cl_transpiler,
        arguments = ["--jvm_flag=-Dj2cl.enable_wasm_checks=" + str(enable_wasm_checks), args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2wasm",
    )

    # Link the wat file for the named output
    ctx.actions.run_shell(
        inputs = [transpile_out],
        outputs = [ctx.outputs.wat],
        # TODO(b/176105504): Link instead copying when Blaze native tree support lands.
        command = "cp %s/module.wat %s" % (transpile_out.path, ctx.outputs.wat.path),
    )

    args = ctx.actions.args()
    args.add("--enable-exception-handling")
    args.add("--enable-typed-function-references")
    args.add("--enable-gc")
    args.add("--enable-reference-types")
    args.add("--enable-sign-ext")
    args.add("--enable-nontrapping-float-to-int")

    args.add("--debuginfo")

    # TODO(b/198219246): remove intermediate invocation to binaryen once Chrome handles nominal wasm types.
    intermediate_wasm_output = ctx.actions.declare_file(ctx.label.name + "_intermediate_wasm_output")
    intermediate_source_map = ctx.actions.declare_file(ctx.label.name + " _intermediate_source_map")

    ctx.actions.run(
        executable = ctx.executable._binaryen,
        arguments = [args] + ctx.attr.binaryen_stage1_args + [
            "--remove-unused-module-elements",
            "-o",
            intermediate_wasm_output.path,
            "--output-source-map",
            intermediate_source_map.path,
            ctx.outputs.wat.path,
        ],
        inputs = [ctx.outputs.wat],
        outputs = [intermediate_source_map, intermediate_wasm_output],
        mnemonic = "J2wasm",
        progress_message = "Compiling to WASM (nominal)",
        # Binaryen can leverage 4 cores with some amount of parallelism.
        execution_requirements = {"cpu:4": ""},
    )

    debug_dir_name = ctx.label.name + "_debug"

    symbolmap = ctx.actions.declare_file(ctx.label.name + ".map")
    args.add("--symbolmap=" + symbolmap.path)
    outputs.append(symbolmap)

    args.add("-o", ctx.outputs.wasm)
    outputs.append(ctx.outputs.wasm)
    runfiles.append(ctx.outputs.wasm)

    source_map_name = ctx.label.name + "_source.map"
    source_map = ctx.actions.declare_file(source_map_name)
    args.add("--output-source-map", source_map)
    outputs.append(source_map)
    args.add("--output-source-map-url", debug_dir_name + "/" + source_map_name)

    ctx.actions.run(
        executable = ctx.executable._binaryen,
        arguments = [args] + ctx.attr.binaryen_stage2_args + [
            "--input-source-map",
            intermediate_source_map.path,
            intermediate_wasm_output.path,
        ],
        inputs = [intermediate_wasm_output, intermediate_source_map],
        outputs = outputs,
        mnemonic = "J2wasm",
        progress_message = "Compiling to WASM",
        # Binaryen can leverage 4 cores with some amount of parallelism.
        execution_requirements = {"cpu:4": ""},
    )

    # Make the debugging data available in runfiles.
    # Note that we are making sure that the sourcemap file is in the root next to
    # others so the relative paths are correct.
    debug_dir = ctx.actions.declare_directory(debug_dir_name)
    runfiles.append(debug_dir)
    ctx.actions.run_shell(
        inputs = [transpile_out, source_map],
        outputs = [debug_dir],
        # TODO(b/176105504): Link instead copy when native tree support lands.
        command = (
            "cp -rL %s/* %s;" % (transpile_out.path, debug_dir.path) +
            "cp %s %s" % (source_map.path, debug_dir.path)
        ),
    )

    # Trigger a parallel Javac build to provide better error messages than JDT.
    ctx.actions.run_shell(
        outputs = [ctx.outputs.validate],
        arguments = [ctx.outputs.validate.path],
        command = "touch $1",
        inputs = _trigger_javac_build(ctx.attr.deps),
    )

    return DefaultInfo(data_runfiles = ctx.runfiles(files = runfiles))

def _get_transitive_srcs(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_srcs for d in deps])

def _get_transitive_classpath(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_classpath for d in deps])

def _trigger_javac_build(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.java_info.transitive_runtime_jars for d in deps])

_j2wasm_application = rule(
    implementation = _impl_j2wasm_application,
    attrs = {
        "deps": attr.label_list(providers = [J2wasmInfo]),
        "entry_points": attr.string_list(),
        "binaryen_stage1_args": attr.string_list(),
        "binaryen_stage2_args": attr.string_list(),
        "transpiler_args": attr.string_list(),
        "defines": attr.string_list(),
        "_jre": attr.label(default = Label("//build_defs/internal_do_not_use:j2wasm_jre")),
        "_j2cl_transpiler": attr.label(
            default = Label(
                "//build_defs/internal_do_not_use:BazelJ2clBuilder",
            ),
            cfg = "host",
            executable = True,
        ),
        "_binaryen": attr.label(
            cfg = "host",
            executable = True,
            default = Label(
                "//build_defs/internal_do_not_use:binaryen",
            ),
        ),
    },
    outputs = {
        "wat": "%{name}.wat",
        "wasm": "%{name}.wasm",
        "validate": "%{name}.validate",
    },
)

def j2wasm_application(name, defines = dict(), **kwargs):
    default_defines = {
        "jre.checkedMode": "ENABLED",
        "jre.checks.checkLevel": "MINIMAL",
        "jre.checks.bounds": "AUTO",
        "jre.checks.api": "AUTO",
        "jre.checks.numeric": "AUTO",
        "jre.checks.type": "AUTO",
        "jre.logging.logLevel": "ALL",
        "jre.logging.simpleConsoleHandler": "ENABLED",
    }

    dev_defines = dict(default_defines)
    dev_defines.update(defines)

    optimized_defines = dict(default_defines)
    optimized_defines.update({
        "jre.checkedMode": "DISABLED",
        "jre.logging.logLevel": "SEVERE",
        "jre.logging.simpleConsoleHandler": "DISABLED",
    })
    optimized_defines.update(defines)

    _j2wasm_application(
        name = name,
        binaryen_stage1_args = [
            # Optimization flags (affecting passes in general) included at top.
            "--traps-never-happen",
            "--nominal",
            # Specific list of passes: The order and count of these flags does
            # matter. First -O3 will be the slowest, so we isolate it in a
            # stage1 invocation (due to go/forge-limits for time).
            "-O3",
        ],
        binaryen_stage2_args = [
            "--traps-never-happen",
            "--partial-inlining-ifs=4",
            "-fimfs=50",
            # Get several rounds of -O3 before intrinsic lowering.
            "-O3",
            "-O3",
            "-O3",
            "--intrinsic-lowering",
            # Get several rounds of -O3 after intrinsic lowering.
            "-O3",
            "-O3",
            "-O3",
        ],
        transpiler_args = ["-experimentalWasmRemoveAssertStatement"],
        defines = ["%s=%s" % (k, v) for (k, v) in optimized_defines.items()],
        **kwargs
    )
    _j2wasm_application(
        name = name + "_dev",
        binaryen_stage1_args = ["--nominal", "--intrinsic-lowering"],
        defines = ["%s=%s" % (k, v) for (k, v) in dev_defines.items()],
        **kwargs
    )

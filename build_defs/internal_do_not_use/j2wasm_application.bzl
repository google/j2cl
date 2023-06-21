"""j2wasm_application build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":provider.bzl", "J2wasmInfo")
load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "j2cl_js_provider")

# Template for the generated JS imports file.
# The `getImports` function referenced by `instantiateStreaming` is defined by the Wasm backend.
_JS_IMPORTS_TEMPLATE = """// GENERATED CODE.
goog.module("%MODULE_NAME%.j2wasm");

%IMPORTS%

/**
 * Instantiates the web assembly module.
 *
 * @param {string|!Promise<!Response>} urlOrResponse
 * @return {!Promise<!WebAssembly.Instance>}
 */
async function instantiateStreaming(urlOrResponse) {
    const response =
        typeof urlOrResponse == "string" ? fetch(urlOrResponse) : urlOrResponse;
    const {instance} = await WebAssembly.instantiateStreaming(response, getImports());
    return instance;
}

/**
 * Instantiates a web assembly module passing the necessary imports and any
 * additional import the user might need to provide for their application.
 *
 * Use of this function is discouraged. Many browsers require when calling the
 * WebAssembly constructor that the number of bytes of the module is under a
 * small threshold, mandating the async functions for all non-trivial apps. This
 * function can be used in other contexts, such as the D8 command line.
 *
 * @param {!BufferSource} moduleObject
 * @return {!WebAssembly.Instance}
 */
function instantiateBlocking(moduleObject) {
    return new WebAssembly.Instance(new WebAssembly.Module(moduleObject), getImports());
}

exports = {instantiateStreaming, instantiateBlocking};
"""

def _impl_j2wasm_application(ctx):
    deps = ctx.attr.deps + [ctx.attr._jre]
    srcs = _get_transitive_srcs(deps)
    classpath = _get_transitive_classpath(deps)
    module_outputs = _get_transitive_modules(deps)

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

    ctx.actions.run(
        progress_message = "Transpiling to Wasm %s" % ctx.label,
        inputs = depset(transitive = [srcs, classpath]),
        outputs = [transpile_out],
        executable = ctx.executable._j2cl_transpiler,
        arguments = [args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2wasmTranspile",
    )

    # Link the wat file for the named output
    ctx.actions.run_shell(
        inputs = [transpile_out],
        outputs = [ctx.outputs.wat],
        # TODO(b/176105504): Link instead copying when Blaze native tree support lands.
        command = "cp %s/module.wat %s" % (transpile_out.path, ctx.outputs.wat.path),
    )

    # Link the imports JS file for the named output.
    ctx.actions.run_shell(
        inputs = [transpile_out],
        outputs = [ctx.outputs.jsimports],
        # TODO(b/176105504): Link instead copying when Blaze native tree support lands.
        command = "cp %s/imports.txt %s" % (transpile_out.path, ctx.outputs.jsimports.path),
    )

    # Bundle the module outputs.
    bundler_args = ctx.actions.args()
    bundler_args.add_all(module_outputs.to_list(), expand_directories = False)
    bundler_args.add("-output", ctx.outputs.bundle)
    ctx.actions.run(
        progress_message = "Bundling modules for Wasm %s" % ctx.label,
        inputs = module_outputs,
        outputs = [ctx.outputs.bundle],
        executable = ctx.executable._bundler,
        arguments = [bundler_args],
        env = dict(LANG = "en_US.UTF-8"),
        execution_requirements = {"supports-workers": "1"},
        mnemonic = "J2wasm",
    )

    debug_dir_name = ctx.label.name + "_debug"
    source_map_base_url = ctx.attr.source_map_base_url or debug_dir_name

    input = ctx.outputs.wat
    input_source_map = None
    stages = _extract_stages(ctx.attr.binaryen_args)
    current_stage = 0
    for stage_args in stages:
        current_stage += 1

        args = ctx.actions.args()
        args.add("--enable-exception-handling")
        args.add("--enable-gc")
        args.add("--enable-reference-types")
        args.add("--enable-sign-ext")
        args.add("--enable-strings")
        args.add("--enable-nontrapping-float-to-int")
        args.add("--enable-bulk-memory")
        args.add("--closed-world")
        args.add("--traps-never-happen")
        args.add_all(stage_args)

        inputs = []
        outputs = []

        if current_stage == len(stages):
            # last stage
            output = ctx.outputs.wasm
            output_source_map = ctx.outputs.srcmap

            # Add the extra flags that are only needed in for the final run.
            args.add("--output-source-map-url", source_map_base_url + "/" + ctx.outputs.srcmap.basename)

            # SymbolMap flag must be after optimization passes to get the final symbol names.
            args.add("--symbolmap=" + ctx.outputs.symbolmap.path)
            outputs.append(ctx.outputs.symbolmap)

            # Always maintain debug information if explicitly asked by user.
            if ctx.var.get("J2CL_APP_STYLE", "") == "PRETTY":
                args.add("--debuginfo")
        else:
            output = ctx.actions.declare_file(ctx.label.name + "_intermediate_%s.wasm" % current_stage)
            output_source_map = ctx.actions.declare_file(ctx.label.name + "_intermediate_%s_map" % current_stage)

            # Maintain debug information in intermediate stages.
            args.add("--debuginfo")

        args.add("-o", output)
        outputs.append(output)
        args.add("--output-source-map", output_source_map)
        outputs.append(output_source_map)

        if input_source_map:
            args.add("--input-source-map", input_source_map)
            inputs.append(input_source_map)
        args.add(input)
        inputs.append(input)

        ctx.actions.run(
            executable = ctx.executable._binaryen,
            arguments = [args],
            inputs = inputs,
            outputs = outputs,
            mnemonic = "J2wasm",
            progress_message = "Compiling to Wasm (stage %s)" % current_stage,
            # Binaryen can leverage 4 cores with some amount of parallelism.
            execution_requirements = {"cpu:4": ""},
        )

        # Outputs of stage 'n' are now the inputs for stage 'n+1'
        input = output
        input_source_map = output_source_map

    runfiles.append(ctx.outputs.wasm)

    # Make the debugging data available in runfiles.
    # Note that we are making sure that the sourcemap file is in the root next to
    # others so the relative paths are correct.
    debug_dir = ctx.actions.declare_directory(debug_dir_name)
    runfiles.append(debug_dir)
    ctx.actions.run_shell(
        inputs = [transpile_out, ctx.outputs.srcmap, ctx.outputs.symbolmap],
        outputs = [debug_dir],
        # TODO(b/176105504): Link instead copy when native tree support lands.
        command = (
            "cp -rL %s/* %s;" % (transpile_out.path, debug_dir.path) +
            "cp %s %s %s" % (ctx.outputs.srcmap.path, ctx.outputs.symbolmap.path, debug_dir.path)
        ),
    )

    # Make the actual JS imports mapping file using the template.
    js_module = ctx.actions.declare_file(ctx.label.name + ".js")
    ctx.actions.run_shell(
        inputs = [ctx.outputs.jsimports],
        outputs = [js_module],
        command = "echo '%s' " % _JS_IMPORTS_TEMPLATE +
                  "| sed -e 's/%%MODULE_NAME%%/%s/g' " % ctx.label.name.replace("-", "_") +
                  "| sed -e '/%%IMPORTS%%/r %s' -e '//d ' " % ctx.outputs.jsimports.path +
                  ">> %s" % js_module.path,
    )

    # Build a JS provider exposing the JS imports mapping.
    js_info = j2cl_js_provider(
        ctx,
        srcs = [js_module],
        deps = [d[J2wasmInfo]._private_.js_info for d in deps],
    )

    return [
        DefaultInfo(
            files = depset([
                ctx.outputs.wat,
                ctx.outputs.wasm,
                ctx.outputs.srcmap,
                ctx.outputs.jsimports,
                ctx.outputs.symbolmap,
            ]),
            data_runfiles = ctx.runfiles(files = runfiles),
        ),
        OutputGroupInfo(_validation = _trigger_javac_build(ctx.attr.deps)),
        js_info,
    ]

def _get_transitive_srcs(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_srcs for d in deps])

def _get_transitive_classpath(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_classpath for d in deps])

def _get_transitive_modules(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.wasm_modular_info.transitive_modules for d in deps])

_STAGE_SEPARATOR = "--NEW_STAGE--"

def _extract_stages(args):
    current_stage_args = []
    stages = [current_stage_args]
    for arg in args:
        if arg == _STAGE_SEPARATOR:
            current_stage_args = []
            stages.append(current_stage_args)
        else:
            current_stage_args.append(arg)
    return stages

# Trigger a parallel Javac build to provide better error messages than JDT.
def _trigger_javac_build(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.java_info.transitive_runtime_jars for d in deps])

_J2WASM_APP_ATTRS = {
    "deps": attr.label_list(providers = [J2wasmInfo]),
    "entry_points": attr.string_list(),
    "binaryen_args": attr.string_list(),
    "transpiler_args": attr.string_list(),
    "defines": attr.string_list(),
    "source_map_base_url": attr.string(),
    "_jre": attr.label(default = Label("//build_defs/internal_do_not_use:j2wasm_jre")),
    "_j2cl_transpiler": attr.label(
        cfg = "exec",
        executable = True,
        default = Label(
            "//build_defs/internal_do_not_use:BazelJ2clBuilder",
        ),
    ),
    "_binaryen": attr.label(
        cfg = "exec",
        executable = True,
        default = Label(
            "//build_defs/internal_do_not_use:binaryen",
        ),
    ),
    "_bundler": attr.label(
        cfg = "exec",
        executable = True,
        default = Label(
            "//build_defs/internal_do_not_use:J2wasmBundler",
        ),
    ),
}
_J2WASM_APP_ATTRS.update(J2CL_JS_TOOLCHAIN_ATTRS)

_j2wasm_application = rule(
    implementation = _impl_j2wasm_application,
    attrs = _J2WASM_APP_ATTRS,
    fragments = ["js"],
    outputs = {
        "wat": "%{name}.wat",
        "wasm": "%{name}.wasm",
        "srcmap": "%{name}.wasm.map",
        "jsimports": "%{name}.imports.js.txt",
        "symbolmap": "%{name}.symbols",
        "bundle": "%{name}.bundle",
    },
)

def j2wasm_application(name, defines = dict(), **kwargs):
    default_defines = {
        "J2WASM_DEBUG": "TRUE",
        "jre.strictFpToString": "DISABLED",
        "jre.checkedMode": "ENABLED",
        "jre.checks.checkLevel": "MINIMAL",
        "jre.checks.bounds": "AUTO",
        "jre.checks.api": "AUTO",
        "jre.checks.numeric": "AUTO",
        "jre.checks.type": "AUTO",
        "jre.logging.logLevel": "ALL",
        "jre.logging.simpleConsoleHandler": "ENABLED",
        "jre.classMetadata": "SIMPLE",
    }

    dev_defines = dict(default_defines)
    dev_defines.update(defines)

    optimized_defines = dict(default_defines)
    optimized_defines.update({
        "J2WASM_DEBUG": "FALSE",
        "jre.checkedMode": "DISABLED",
        "jre.logging.logLevel": "SEVERE",
        "jre.logging.simpleConsoleHandler": "DISABLED",
    })
    optimized_defines.update(defines)

    _j2wasm_application(
        name = name,
        binaryen_args = [
            # Stage 1
            # Specific list of passes: The order and count of these flags does
            # matter. First -O3 will be the slowest, so we isolate it in a
            # stage1 invocation (due to go/forge-limits for time).
            "-O3",
            "--gufa",
            "-O3",

            # Stage 2
            _STAGE_SEPARATOR,
            # Optimization flags (affecting passes in general) included at the beginning of stage.
            "--partial-inlining-ifs=4",
            "-fimfs=50",
            # Specific list of passes:
            "--gufa",
            "-O3",
            "-O3",
            "-O3",
            "--gufa",
            "-O3",

            # Stage 3
            _STAGE_SEPARATOR,
            # Optimization flags (affecting passes in general) included at the beginning of stage.
            "--partial-inlining-ifs=4",
            "-fimfs=50",
            "--intrinsic-lowering",
            "--gufa",
            # Get several rounds of -O3 after intrinsic lowering.
            "-O3",
            "-O3",
        ],
        transpiler_args = ["-experimentalWasmRemoveAssertStatement"],
        defines = ["%s=%s" % (k, v) for (k, v) in optimized_defines.items()],
        **kwargs
    )
    _j2wasm_application(
        name = name + "_dev",
        binaryen_args = [
            "--debuginfo",
            "--intrinsic-lowering",
            # Remove the intrinsic import declarations which are not removed by lowering itself.
            "--remove-unused-module-elements",
        ],
        defines = ["%s=%s" % (k, v) for (k, v) in dev_defines.items()],
        **kwargs
    )

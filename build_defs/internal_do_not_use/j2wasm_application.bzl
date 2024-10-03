"""j2wasm_application build macro

Takes Java source, translates it into Wasm.
This is an experimental tool and should not be used.
"""

load(":j2cl_js_common.bzl", "J2CL_JS_TOOLCHAIN_ATTRS", "j2cl_js_provider")
load(":provider.bzl", "J2wasmInfo")

# Template for the generated JS imports file.
# The `getImports` function referenced by `instantiateStreaming` is defined by the Wasm backend.
_JS_IMPORTS_TEMPLATE = """// GENERATED CODE.
goog.module("%MODULE_NAME%.j2wasm");

%IMPORTS%

const options = { "builtins": ["js-string"] , "importedStringConstants": "'"'"'" }

/**
 * Instantiates the web assembly module. This is the recommended way to load & instantate
 * Wasm module.
 *
 * @param {string|!Response|!Promise<!Response>} urlOrResponse
 * @return {!Promise<!WebAssembly.Instance>}
 * @suppress {checkTypes} Externs are missing options parameter (phase 2)
 */
async function instantiateStreaming(urlOrResponse) {
  const useMagicStringImports = %USE_MAGIC_STRING_IMPORTS%;
  if (useMagicStringImports) {
    // Shortcut for magic import case.
    const response = typeof urlOrResponse == "string" ? fetch(urlOrResponse) : urlOrResponse;
    const {instance} = await WebAssembly.instantiateStreaming(response, getImports(), options);
    return instance;
  }
  const module = await compileStreaming(urlOrResponse);
  return instantiate(module);
}

/**
 * @param {string|!Response|!Promise<!Response>} urlOrResponse
 * @return {!Promise<!WebAssembly.Module>}
 * @suppress {checkTypes} Externs are missing options parameter (phase 2)
 */
async function compileStreaming(urlOrResponse) {
  const response =
      typeof urlOrResponse == "string" ? fetch(urlOrResponse) : urlOrResponse;
  return WebAssembly.compileStreaming(response, options);
}

/**
 * @param {!WebAssembly.Module} module
 * @return {!Promise<!WebAssembly.Instance>}
 * @suppress {checkTypes} Externs are missing overloads for WebAssembly.instantiate.
 */
async function instantiate(module) {
  return WebAssembly.instantiate(module, prepareImports(module));
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
 * @suppress {checkTypes} Externs are missing options parameter (phase 2)
 */
function instantiateBlocking(moduleObject) {
  const module = new WebAssembly.Module(moduleObject, options);
  return new WebAssembly.Instance(module, prepareImports(module));
}

/**
 * @param {!WebAssembly.Module} module
 * @return {!Object<string, *>}
 */
function prepareImports(module) {
  const imports = getImports();
  const stringConsts = WebAssembly.Module.customSections(module, "string.consts")[0];
  if (stringConsts) {
    const decodedConsts = new TextDecoder().decode(stringConsts);
    imports["string.const"] = JSON.parse(decodedConsts);
  }
  return imports;
}

exports = {compileStreaming, instantiate, instantiateStreaming, instantiateBlocking};
"""

def _impl_j2wasm_application(ctx):
    deps = [ctx.attr._jre] + ctx.attr.deps
    srcs = _get_transitive_srcs(deps)
    classpath = _get_transitive_classpath(deps)
    module_outputs = _get_transitive_modules(deps)

    runfiles = []
    outputs = []

    transpile_out = ctx.actions.declare_directory(ctx.label.name + "_out")

    if not ctx.attr.use_modular_pipeline:
        # Monolithic pipeline.
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
            mnemonic = "J2wasm",
        )

        # Link the imports JS file for the named output.
        ctx.actions.run_shell(
            inputs = [transpile_out],
            outputs = [ctx.outputs.jsimports],
            # TODO(b/176105504): Link instead copying when Blaze native tree support lands.
            command = "cp %s/imports.txt %s" % (transpile_out.path, ctx.outputs.jsimports.path),
            mnemonic = "J2wasm",
        )
    else:
        # Modular pipeline.

        # Create a module for exports.
        exports_module_output = ctx.actions.declare_directory(ctx.label.name + ".exports")
        exporter_args = ctx.actions.args()
        exporter_args.use_param_file("@%s", use_always = True)
        exporter_args.set_param_file_format("multiline")
        exporter_args.add_joined("-classpath", _get_all_classjars(deps).to_list(), join_with = ctx.configuration.host_path_separator)
        exporter_args.add("-output", exports_module_output.path)
        exporter_args.add_all(ctx.attr.entry_points, before_each = "-entryPointPattern")
        ctx.actions.run(
            progress_message = "Generating Wasm Exports %s" % ctx.label,
            inputs = _get_all_classjars(deps),
            outputs = [exports_module_output],
            executable = ctx.executable._export_generator,
            arguments = [exporter_args],
            env = dict(LANG = "en_US.UTF-8"),
            execution_requirements = {"supports-workers": "1"},
            mnemonic = "J2wasm",
        )

        all_modules = module_outputs.to_list() + [exports_module_output]
        jre_jars = ctx.attr._jre[J2wasmInfo]._private_.java_info.compile_jars.to_list()

        # Bundle the module outputs.
        bundler_args = ctx.actions.args()
        bundler_args.use_param_file("@%s", use_always = True)
        bundler_args.set_param_file_format("multiline")
        bundler_args.add_all(all_modules, expand_directories = False)
        bundler_args.add_joined("-classpath", jre_jars, join_with = ctx.configuration.host_path_separator)
        bundler_args.add_all(ctx.attr.defines, before_each = "-define")
        bundler_args.add("-output", ctx.outputs.wat)
        bundler_args.add("-jsimports", ctx.outputs.jsimports)
        ctx.actions.run(
            progress_message = "Bundling modules for Wasm %s" % ctx.label,
            # Note that all_modules also contains some files that are not
            # actually needed by the bundler, e.g. namemaps; that increases
            # the total size of the inputs to the bundler.
            inputs = all_modules + jre_jars,
            outputs = [ctx.outputs.wat, ctx.outputs.jsimports],
            executable = ctx.executable._bundler,
            arguments = [bundler_args],
            env = dict(LANG = "en_US.UTF-8"),
            execution_requirements = {"supports-workers": "1"},
            mnemonic = "J2wasm",
        )

        ctx.actions.run_shell(
            inputs = all_modules,
            outputs = [transpile_out],
            command = "mkdir -p %s && cat %s > %s/namemap" % (
                transpile_out.path,
                " ".join([m.path + "/namemap" for m in all_modules]),
                transpile_out.path,
            ),
            mnemonic = "J2wasm",
        )

    source_map_base_url = ctx.attr.source_map_base_url or "."

    input = ctx.outputs.wat
    input_source_map = None
    binaryen_symbolmap = ctx.actions.declare_file(ctx.label.name + ".binaryen.symbolmap")
    binaryen_args = ctx.attr.binaryen_args
    if ctx.attr.use_magic_string_imports:
        magic_imports_flag = "--string-lowering-magic-imports-assert"
        binaryen_args = [magic_imports_flag if x == "--string-lowering" else x for x in binaryen_args]
    stages = _extract_stages(binaryen_args)
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
            args.add("--symbolmap=" + binaryen_symbolmap.path)
            outputs.append(binaryen_symbolmap)

            # Always keep the embedded debug information if explicitly asked by user.
            if ctx.attr.enable_debug_info or ctx.var.get("J2CL_APP_STYLE", "") == "PRETTY":
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

    _remap_symbol_map(ctx, transpile_out, binaryen_symbolmap)

    runfiles.append(ctx.outputs.wasm)
    runfiles.append(ctx.outputs.srcmap)
    runfiles.append(ctx.outputs.symbolmap)
    symlinks = {}

    # Provide the Java sources via symlinks in the runfiles.
    if ctx.attr.use_modular_pipeline:
        # Compute the directory where the source map file will reside (relative to `runtime_root`).
        source_map_short_path_dir = ctx.outputs.srcmap.short_path.removesuffix(ctx.outputs.srcmap.basename)
        for module_output in module_outputs.to_list():
            # Add the module output to the runfiles.
            runfiles.append(module_output)

            # Rebase all source files relative to sourcemap
            module_sourcemap_relative_path = source_map_short_path_dir + module_output.short_path
            symlinks[module_sourcemap_relative_path] = module_output

    # Make the actual JS imports mapping file using the template.
    js_module = ctx.actions.declare_file(ctx.label.name + ".js")
    module_name = ctx.label.name.replace("-", "_")
    use_magic_string_imports = str(ctx.attr.use_magic_string_imports).lower()
    ctx.actions.run_shell(
        inputs = [ctx.outputs.jsimports],
        outputs = [js_module],
        command = "echo '%s' " % _JS_IMPORTS_TEMPLATE +
                  "| sed -e 's/%%MODULE_NAME%%/%s/g' " % module_name +
                  "| sed -e 's/%%USE_MAGIC_STRING_IMPORTS%%/%s/g' " % use_magic_string_imports +
                  "| sed -e '/%%IMPORTS%%/r %s' -e '//d ' " % ctx.outputs.jsimports.path +
                  ">> %s" % js_module.path,
        mnemonic = "J2wasm",
    )

    # Build a JS provider exposing the JS imports mapping.
    js_info = j2cl_js_provider(
        ctx,
        srcs = [js_module],
        deps = [d[J2wasmInfo]._private_.js_info for d in deps],
    )

    return [
        js_info,
        DefaultInfo(
            files = depset([
                ctx.outputs.wat,
                ctx.outputs.wasm,
                ctx.outputs.srcmap,
                ctx.outputs.jsimports,
                ctx.outputs.symbolmap,
            ]),
            data_runfiles = ctx.runfiles(files = runfiles, symlinks = symlinks),
        ),
        OutputGroupInfo(_validation = _trigger_javac_build(ctx.attr.deps)),
    ]

def _get_transitive_srcs(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_srcs for d in deps])

def _get_transitive_classpath(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.transitive_classpath for d in deps])

def _get_transitive_modules(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.wasm_modular_info.transitive_modules for d in deps], order = "postorder")

def _get_all_classjars(deps):
    return depset(transitive = [d[J2wasmInfo]._private_.java_info.transitive_compile_time_jars for d in deps])

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

def _remap_symbol_map(ctx, transpile_out, binaryen_symbolmap):
    ctx.actions.run_shell(
        inputs = [transpile_out, binaryen_symbolmap],
        outputs = [ctx.outputs.symbolmap],
        command =
            """awk 'BEGIN { FS = ":" } {
                from = $1;
                to = substr($0, length(from) + length(FS) + 1);
                if (from ~ /^[0-9]+$/) {
                    symbols[from] = to in mapping ? mapping[to] : to
                } else {
                    mapping[from] = to
                }
            } END {
                for (i in symbols) print i":"symbols[i]
            }' %s/namemap %s > %s""" % (transpile_out.path, binaryen_symbolmap.path, ctx.outputs.symbolmap.path),
        mnemonic = "J2wasm",
    )

_J2WASM_APP_ATTRS = {
    "deps": attr.label_list(providers = [J2wasmInfo]),
    "entry_points": attr.string_list(),
    "binaryen_args": attr.string_list(),
    "transpiler_args": attr.string_list(),
    "defines": attr.string_list(),
    "source_map_base_url": attr.string(),
    # TODO(b/296477606): Remove when symbol map file can be linked from the binary for debugging.
    "enable_debug_info": attr.bool(default = False),
    "use_modular_pipeline": attr.bool(default = True),
    "use_magic_string_imports": attr.bool(),
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
    "_export_generator": attr.label(
        cfg = "exec",
        executable = True,
        default = Label(
            "//build_defs/internal_do_not_use:J2wasmExportGenerator",
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
    },
)

def j2wasm_application(name, defines = dict(), **kwargs):
    default_defines = {
        "J2WASM_DEBUG": "TRUE",
        "jre.strictFpToString": "DISABLED",
        "jre.checkedMode": "ENABLED",
        "jre.checks.checkLevel": "NORMAL",
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
        "jre.checks.checkLevel": "MINIMAL",
        "jre.logging.logLevel": "OFF",
        "jre.logging.simpleConsoleHandler": "DISABLED",
        "jre.classMetadata": "STRIPPED",
    })
    optimized_defines.update(defines)

    transpiler_args = kwargs.pop("internal_transpiler_args", [])

    _j2wasm_application(
        name = name,
        binaryen_args = [
            # Stage 1
            # Optimization flags (affecting passes in general) included at the beginning of stage.
            # Avoid inlining once functions to preserve their shape.
            "--no-inline=*_<once>_*",
            # Specific list of passes: The order and count of these flags does
            # matter. First -O3 will be the slowest, so we isolate it in a
            # stage1 invocation (due to go/forge-limits for time).
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "--gufa",
            "--unsubtyping",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",

            # Stage 2
            _STAGE_SEPARATOR,
            # Optimization flags (affecting passes in general) included at the beginning of stage.
            # Avoid inlining once functions to preserve their shape.
            "--no-inline=*_<once>_*",
            "--partial-inlining-ifs=4",
            "-fimfs=25",
            # Specific list of passes:
            "--gufa",
            "--unsubtyping",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "--gufa",
            "--unsubtyping",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",

            # Stage 3
            _STAGE_SEPARATOR,
            # Optimization flags (affecting passes in general) included at the beginning of stage.
            # Only allow partial inlining since they only executed once.
            "--no-full-inline=*_<once>_*",
            "--partial-inlining-ifs=4",
            "--intrinsic-lowering",
            "--gufa",
            "--unsubtyping",
            # Get several rounds of -O3 after intrinsic lowering.
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",
            "-O3",
            "--optimize-j2cl",
            "--cfp-reftest",
            "--type-merging",
            "-O3",
            "--cfp-reftest",
            "--optimize-j2cl",

            # Final clean-ups.
            "--string-lowering",
            "--remove-unused-module-elements",
            "--reorder-globals",

            # Mark all types as 'final' that we can, to help VMs at runtime.
            "--type-finalizing",
        ],
        transpiler_args = transpiler_args,
        defines = ["%s=%s" % (k, v) for (k, v) in optimized_defines.items()],
        **kwargs
    )
    _j2wasm_application(
        name = name + "_dev",
        binaryen_args = [
            "--debuginfo",
            "--intrinsic-lowering",
            "--string-lowering",
            # Remove the intrinsic import declarations which are not removed by lowering itself.
            "--remove-unused-module-elements",
        ],
        transpiler_args = transpiler_args,
        defines = ["%s=%s" % (k, v) for (k, v) in dev_defines.items()],
        **kwargs
    )

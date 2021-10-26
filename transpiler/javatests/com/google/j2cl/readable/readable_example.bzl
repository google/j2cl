"""readable_example build macro

Confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
)

"""

load("@io_bazel_rules_closure//closure:defs.bzl", "js_binary")
load(
    "//build_defs:rules.bzl",
    "J2CL_OPTIMIZED_DEFS",
    "j2cl_library",
    "j2kt_library",
    "j2wasm_application",
)
load("@bazel_skylib//rules:build_test.bzl", "build_test")

JAVAC_FLAGS = [
    "-XepDisableAllChecks",
]

# TODO(dpo): instead of getting explicit kt_deps, we normally would auto generate them via a naming
# convention (e.g. replace foo-j2cl with foo-j2kt)
def readable_example(
        srcs,
        deps = [],
        kt_deps = [],
        plugins = [],
        defs = [],
        generate_library_info = False,
        j2cl_library_tags = [],
        javacopts = [],
        generate_wasm_readables = True,
        wasm_entry_points = [],
        generate_kt_readables = True,
        build_kt_readables = True,
        **kwargs):
    """Macro that confirms the JS compilability of some transpiled Java.

    Args:
      srcs: Source files to make readable output for.
      deps: J2CL libraries referenced by the srcs.
      plugins: APT processors to execute when generating readable output.
      defs: Custom flags to pass to the JavaScript compiler.
      generate_library_info: Wheter to copy the call graph for the library in the output dir.
      j2cl_library_tags: Tags to apply j2cl_library
      javacopts: javacopts to apply j2cl_library
      **kwargs: passes to j2cl_library
    """

    # Transpile the Java files.
    j2cl_library(
        name = "readable",
        srcs = srcs,
        javacopts = JAVAC_FLAGS + javacopts,
        deps = deps,
        plugins = plugins,
        generate_build_test = False,
        tags = j2cl_library_tags,
        readable_source_maps = True,
        readable_library_info = generate_library_info,
        **kwargs
    )

    _readable_diff_test(
        name = "readable_golden",
        target = ":readable.js",
        dir_out = "output_closure",
        tags = ["j2cl"],
    )

    # Verify compilability of generated JS.
    js_binary(
        name = "readable_binary",
        defs = J2CL_OPTIMIZED_DEFS + [
            "--conformance_config=transpiler/javatests/com/google/j2cl/readable/conformance_proto.txt",
            "--jscomp_warning=conformanceViolations",
            "--jscomp_warning=strictPrimitiveOperators",
            "--summary_detail_level=3",
        ] + defs,
        compiler = "//javascript/tools/jscompiler:head",
        extra_inputs = ["//transpiler/javatests/com/google/j2cl/readable:conformance_proto"],
        deps = [":readable"],
    )

    build_test(
        name = "readable_build_test",
        targets = ["readable_binary"],
        tags = ["j2cl"],
    )

    if generate_wasm_readables:
        j2wasm_application(
            name = "readable_wasm",
            deps = [":readable-j2wasm"],
            entry_points = wasm_entry_points,
        )

        _readable_diff_test(
            name = "readable_wasm_golden",
            target = ":readable_wasm.wat",
            dir_out = "output_wasm",
            tags = ["j2wasm"],
        )

        build_test(
            name = "readable_wasm_build_test",
            targets = ["readable_wasm"],
            tags = ["j2wasm"],
        )

    if generate_kt_readables:
        j2kt_library(
            name = "readable_kt",
            srcs = srcs,
            deps = kt_deps,
            # TODO(b/202767120): Allow separate flags for JavaOpts and Kotlin opts.
            javacopts = JAVAC_FLAGS + javacopts,
            plugins = plugins,
            **kwargs
        )

        _readable_diff_test(
            name = "readable_kt_golden",
            target = ":readable_kt.kt",
            dir_out = "output_kt",
            tags = ["j2kt"],
        )

        if build_kt_readables:
            build_test(
                name = "readable_kt_build_test",
                targets = [":libreadable_kt.kt.jar"],
                tags = ["j2kt"],
            )

def _readable_diff_test(name, target, dir_out, tags):
    _golden_output(
        name = name,
        target = target,
    )

    # TODO: add open source diff test

def _golden_output_impl(ctx):
    input = ctx.file.target
    output = ctx.actions.declare_directory(ctx.label.name)
    readable_name = ctx.label.package.rsplit("/", 1)[1]

    if input.path.endswith(".wat"):
        ctx.actions.run_shell(
            inputs = [input],
            outputs = [output],
            command = "".join([
                "awk 'BEGIN {firstMatch=1} {",
                " if (match($$0, /\\s*;;; Code for %s/)) {" % readable_name,
                "  if (firstMatch) { firstMatch=0 } else { printf \"\\n\" }",
                "  inPackage=1;",
                " };",
                " if (match($$0, /\\s*;;; End of /)) {inPackage=0};",
                " if (inPackage) {print $$0}",
                "}' < %s > %s/module.wat.txt" % (input.path, output.path),
            ]),
        )
    else:
        ctx.actions.run_shell(
            inputs = [input],
            outputs = [output],
            command = "\n".join([
                "set -e",
                "INPUT=%s" % input.path,
                "OUTPUT=%s" % output.path,
                "cp -L -rf ${INPUT}/* ${OUTPUT}",
                "cd ${OUTPUT}",
                # We don't want to copy .java and .map files to the final output.
                "find \\( -name *.java -o -name *.map \\) -exec rm {} \\;",
                # Rename all files to => {file}.txt
                "find -type f -exec mv {} {}.txt \\;",
                # Normalize the path relative to readable_name to avoid extra dirs.
                "mv ./%s/* ./" % readable_name,
            ]),
        )

    return DefaultInfo(files = depset([output]), runfiles = ctx.runfiles([output]))

_golden_output = rule(
    implementation = _golden_output_impl,
    attrs = {"target": attr.label(allow_single_file = True)},
)

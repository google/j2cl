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
load(":readable_diff_test.bzl", "make_diff_test")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "get_java_package", "get_java_path")

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

    make_diff_test(
        name = "readable_test",
        base_targets = native.glob(["output_closure/**/*.txt"]),
        base = native.package_name() + "/output_closure/",
        test_input_targets = [":readable.js"],
        test_input = "$(location :readable.js)",
        test_base_path = get_java_path(native.package_name()),
    )

    if generate_wasm_readables:
        j2wasm_application(
            name = "readable_wasm",
            deps = [":readable-j2wasm"],
            entry_points = wasm_entry_points,
        )

        build_test(
            name = "readable_wasm_build_test",
            targets = ["readable_wasm"],
            tags = ["j2wasm"],
        )

        java_package = get_java_package(native.package_name())
        native.genrule(
            name = "readable_wasm_filtered",
            srcs = [":readable_wasm.wat"],
            outs = ["readable_wasm.filtered.wat"],
            cmd = ("awk 'BEGIN {firstMatch=1} {" +
                   " if (match($$0, /\\s*;;; Code for %s/)) {" % java_package +
                   "  if (firstMatch) { firstMatch=0 } else { printf \"\\n\" }" +
                   "  inPackage=1;" +
                   " };" +
                   " if (match($$0, /\\s*;;; End of /)) {inPackage=0};" +
                   " if (inPackage) {print $$0}" +
                   "}' < $(SRCS) > $(OUTS)"),
            output_to_bindir = 1,
        )

        make_diff_test(
            name = "readable_wasm_test",
            base_targets = ["output_wasm/module.wat.txt"],
            base = native.package_name() + "/output_wasm/module.wat.txt",
            test_input_targets = [":readable_wasm.filtered.wat"],
            test_input = "$(location :readable_wasm.filtered.wat)",
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

        if build_kt_readables:
            build_test(
                name = "readable_kt_build_test",
                targets = [":libreadable_kt.kt.jar"],
                tags = ["j2kt"],
            )

        make_diff_test(
            name = "readable_kt_test",
            base_targets = native.glob(["output_kt/**/*.txt"]),
            base = native.package_name() + "/output_kt/",
            test_input_targets = [":readable_kt.kt"],
            test_input = "$(location :readable_kt.kt)",
            test_base_path = get_java_path(native.package_name()),
        )

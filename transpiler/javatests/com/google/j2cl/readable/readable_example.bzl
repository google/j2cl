"""readable_example build macro

Confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
)

"""

load("@rules_cc//cc:objc_library.bzl", "objc_library")
load("//third_party/bazel_rules/rules_kotlin/kotlin/native:kt_ios.bzl", "kt_ios_build_test")
load("@rules_shell//shell:sh_test.bzl", "sh_test")
load("@bazel_skylib//rules:write_file.bzl", "write_file")
load(
    "//build_defs:rules.bzl",
    "J2CL_OPTIMIZED_DEFS",
    "j2cl_library",
    "j2wasm_application",
)
load("//build_defs/internal_do_not_use:j2cl_common.bzl", "j2cl_common")
load("//build_defs/internal_do_not_use:j2kt_web_transition.bzl", "j2kt_web_transition")
load("//build_defs/internal_do_not_use:j2wasm_common.bzl", "J2WASM_FEATURE_SET")
load("//build_defs/internal_do_not_use:provider.bzl", "J2clInfo", "J2wasmInfo")
load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_closure//closure:defs.bzl", "closure_js_binary")

JAVAC_FLAGS = [
    "-XepDisableAllChecks",
]

def readable_example(
        srcs,
        deps = [],
        plugins = [],
        defs = [],
        generate_library_info = False,
        j2cl_library_tags = [],
        javacopts = [],
        generate_js_readables = True,
        generate_readable_source_maps = False,
        generate_wasm_readables = True,
        generate_wasm_imports = False,
        generate_wasm_externs = False,
        wasm_entry_points = [],
        generate_kt_readables = True,
        generate_kt_web_readables = False,
        build_kt_readables = True,
        build_kt_native_readables = True,
        j2kt_j2objc_interop_enabled = False,
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

    if "readable/kotlin" in native.package_name():
        # J2KT doesn't make sense for Kotlin Frontend.
        generate_kt_readables = False

        # Wasm is currently not planned for Kotlin Frontend.
        generate_wasm_readables = False

    build_kt_native_readables = generate_kt_readables and build_kt_readables and build_kt_native_readables
    generate_kt_web_readables = generate_kt_readables and generate_kt_web_readables

    # Transpile the Java files.
    j2cl_library(
        name = "readable",
        srcs = srcs,
        javacopts = JAVAC_FLAGS + javacopts,
        deps = deps,
        plugins = plugins,
        generate_build_test = False,
        tags = j2cl_library_tags + ["manual"],
        readable_source_maps = generate_readable_source_maps,
        readable_library_info = generate_library_info,
        generate_j2kt_jvm_library = None if generate_kt_readables else False,
        generate_j2kt_native_library = None if build_kt_native_readables else False,
        generate_j2wasm_library = None if generate_wasm_readables else False,
        j2kt_j2objc_interop_enabled = j2kt_j2objc_interop_enabled,
        **kwargs
    )

    if generate_js_readables:
        # this is just an alias so that we can disable the readable golden generation in replace_all.py.
        native.alias(
            name = "readable_js",
            actual = ":readable",
        )

        _js_readable_targets("readable", "output_closure", defs)

    if generate_wasm_readables:
        _wasm_readable_targets(
            feature_set = J2WASM_FEATURE_SET.DEFAULT if not generate_wasm_externs else J2WASM_FEATURE_SET.CUSTOM_DESCRIPTORS_JSINTEROP,
            entry_points = wasm_entry_points,
            generate_imports = generate_wasm_imports,
        )

    if generate_kt_readables:
        _readable_diff_test(
            name = "readable_j2kt_golden",
            target = ":readable-j2kt-jvm.kt-all",
            dir_out = "output_kt",
            tags = ["j2kt"],
        )

        if build_kt_readables:
            build_test(
                name = "readable_j2kt_jvm_build_test",
                targets = [":libreadable-j2kt-jvm.kt.jar"],
                tags = ["j2kt"],
            )

        if build_kt_native_readables:
            # Generate a objective library to force parsing of the header file.
            write_file(
                name = "ParseHeaders_m",
                out = "ParseHeaders.m",
                content = ["""#import "%s/%s.h" """ % (native.package_name(), src[:-5]) for src in srcs if src.endswith(".java")],
                tags = ["j2kt", "ios", "manual"],
            )
            objc_library(
                name = "ios_parse_headers",
                testonly = 1,
                srcs = ["ParseHeaders.m"],
                tags = ["j2kt", "ios", "manual"],
                deps = [
                    ":readable-j2kt-native",
                ],
            )

            kt_ios_build_test(
                name = "readable_j2kt_native_build_test",
                targets = [":ios_parse_headers"],
                minimum_os_version = "12.0",
                tags = ["manual", "j2kt", "ios"],
            )

    if generate_kt_web_readables:
        _j2kt_web_enabled_j2cl_library(
            name = "readable-j2kt-web",
            j2cl_library = ":readable",
        )

        # Expose the generated js files under the expected name.
        native.alias(
            name = "readable-j2kt-web.js",
            actual = ":readable-j2kt-web",
        )

        _js_readable_targets("readable-j2kt-web", "output_j2kt_web", defs)

def _js_readable_targets(readable_target, dir_out, defs):
    _readable_diff_test(
        name = "%s_golden" % readable_target,
        target = ":%s.js" % readable_target,
        dir_out = dir_out,
        tags = ["j2cl"],
    )

    # Verify compatibility of generated JS.
    closure_js_binary(
        name = "%s_binary" % readable_target,
        defs = J2CL_OPTIMIZED_DEFS + [
            "--conformance_config=transpiler/javatests/com/google/j2cl/readable/conformance_proto.txt",
            "--jscomp_warning=conformanceViolations",
            "--jscomp_warning=strictPrimitiveOperators",
            "--jscomp_warning=checkRegExp",
            "--jscomp_warning=checkTypes",
            "--jscomp_warning=const",
            "--jscomp_warning=missingProperties",
            "--jscomp_warning=tooManyTypeParams",
            "--jscomp_warning=visibility",
            "--summary_detail_level=3",
        ] + defs,
        compiler = "//javascript/tools/jscompiler:head",
        extra_inputs = ["//transpiler/javatests/com/google/j2cl/readable:conformance_proto"],
        use_precompiled_libraries = False,
        deps = [":%s" % readable_target],
    )

    build_test(
        name = "%s_build_test" % readable_target,
        targets = ["%s_binary" % readable_target],
        tags = ["j2cl"],
    )

def _wasm_readable_targets(feature_set, entry_points, generate_imports):
    _feature_set_enabled_j2wasm_library(
        name = "readable-j2wasm-feature_set",
        j2wasm_library = ":readable-j2wasm",
        feature_set = feature_set,
    )

    j2wasm_application(
        name = "readable_wasm",
        deps = [":readable-j2wasm-feature_set"],
        entry_points = entry_points,
        feature_set = feature_set,
    )

    _readable_diff_test(
        name = "readable_wasm_golden",
        target = ":readable-j2wasm-feature_set",
        target_file = "readable-j2wasm.js",
        dir_out = "output_wasm",
        tags = ["j2wasm"],
    )

    if generate_imports:
        _readable_diff_test(
            name = "readable_wasm_imports_golden",
            target = ":readable_wasm.imports.js.txt",
            dir_out = "output_wasm_imports",
            tags = ["j2wasm"],
        )

    build_test(
        name = "readable_wasm_build_test",
        targets = ["readable_wasm"],
        tags = ["j2wasm"],
    )

def _readable_diff_test(name, target, dir_out, tags, target_file = ""):
    _golden_output(
        testonly = 1,
        name = name,
        target = target,
        target_file = target_file,
    )

    sh_test(
        name = name + "_test",
        srcs = ["//transpiler/javatests/com/google/j2cl/readable:diff_check"],
        data = native.glob(["%s/**" % dir_out]) + [name],
        args = [
            '"%s/%s"' % (native.package_name(), dir_out),
            '"$(location %s)"' % name,
        ],
        tags = tags + ["golden"],
    )

def _golden_output_impl(ctx):
    if ctx.attr.target_file:
        # Find the target file or directory
        input = [f for f in ctx.files.target if f.basename == ctx.attr.target_file][0]
    else:
        if len(ctx.files.target) != 1:
            fail("target_file is required if not exactly one output: %s" % ctx.attr.target.label)
        input = ctx.files.target[0]

    output = ctx.actions.declare_directory(ctx.label.name)
    readable_name = ctx.label.package.rsplit("/", 1)[1]

    if input.is_directory:
        excluded_extensions = ["java", "map", "binpb"]

        # We exclude kotlin files only if they are not generated by the transpiler (J2KT).
        # 'input' is the output tree artifact of the transpiler.
        if not input.path.endswith(".kt-all"):
            excluded_extensions.append("kt")

        exclusion_filter = " -o ".join(["-name *.%s" % ext for ext in excluded_extensions])

        ctx.actions.run_shell(
            inputs = [input],
            outputs = [output],
            command = "\n".join([
                "set -e",
                "INPUT=%s" % input.path,
                "OUTPUT=%s" % output.path,
                "cp -L -rf ${INPUT}/* ${OUTPUT}",
                "cd ${OUTPUT}",
                # We don't want to copy .java/.kt and .map files to the final output.
                "find \\( %s \\) -exec rm {} \\;" % exclusion_filter,
                # Rename all files to => {file}.txt
                "find -type f -exec mv {} {}.txt \\;",
                # Normalize the path relative to readable_name to avoid extra dirs.
                "mv ./%s/* ./ || true" % readable_name,
            ]),
        )
    elif input.path.endswith(".imports.js.txt"):
        ctx.actions.run_shell(
            inputs = [input],
            outputs = [output],
            command = "cp -L -f %s %s/module.imports.js.txt" % (input.path, output.path),
        )
    else:
        fail("Unsupported input: %s" % input.path)

    return DefaultInfo(files = depset([output]), runfiles = ctx.runfiles([output]))

_golden_output = rule(
    implementation = _golden_output_impl,
    attrs = {
        "target": attr.label(allow_files = True),
        "target_file": attr.string(default = ""),
    },
)

def _j2kt_web_enabled_j2cl_library_impl(ctx):
    j2cl_library = ctx.attr.j2cl_library[0]
    j2cl_provider = j2cl_library[J2clInfo]

    return j2cl_common.create_js_lib_struct(
        # Forward the J2CL and js providers.
        j2cl_provider,
        # Expose the generated Javascript file so `j2` script and other rules defined here
        # can easily extract them.
        [DefaultInfo(files = depset([j2cl_provider._private_.output_js]))],
    )

_j2kt_web_enabled_j2cl_library = rule(
    implementation = _j2kt_web_enabled_j2cl_library_impl,
    attrs = {
        "j2cl_library": attr.label(providers = [J2clInfo], cfg = j2kt_web_transition),
    },
)

def _feature_set_enabled_j2wasm_library_impl(ctx):
    j2wasm_library = ctx.attr.j2wasm_library[0]
    j2wasm_provider = j2wasm_library[J2wasmInfo]
    default_provider = j2wasm_library[DefaultInfo]
    return [default_provider, j2wasm_provider]

_j2wasm_feature_set_transition = transition(
    implementation = lambda settings, attr: {"//build_defs/internal_do_not_use:j2wasm_feature_set": attr.feature_set},
    inputs = [],
    outputs = ["//build_defs/internal_do_not_use:j2wasm_feature_set"],
)

_feature_set_enabled_j2wasm_library = rule(
    implementation = _feature_set_enabled_j2wasm_library_impl,
    attrs = {
        "j2wasm_library": attr.label(providers = [J2wasmInfo, DefaultInfo], cfg = _j2wasm_feature_set_transition),
        "feature_set": attr.string(default = J2WASM_FEATURE_SET.DEFAULT),
    },
)

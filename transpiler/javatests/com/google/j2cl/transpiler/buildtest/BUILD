# Description:
#   Various BUILD configuration tests.
#
# No example of Pinto usage *without* PMF Enhancer is provided since PMF
# Enhancer must be present to handle references to zip files in the .PMF file
# before MSS sees them.
#
# Put another way, if you're using J2CL with MSS, you *must* also be using
# PMF Enhancer.
#
# TODO(stalcup): add a Boq web example when Boq web is ready.

load("//apps/framework/modulesets/builddefs:modulesets.bzl", "mss_js_compiled_module_set_flat_files")
load("//java/com/google/devtools/javascript/pinto/builddefs:rules.bzl", "pinto_processed_module_set")
load("//build_defs:rules.bzl", "j2cl_test")

# Apache2
licenses(["notice"])

# ********************************************
# Broken target with notap and manual tags should not break

j2cl_test(
    name = "BrokenTest",
    srcs = ["NotExists.java"],
    tags = [
        "manual",
        "notap",
    ],
)

# To demonstrate java_library behaves the same
java_library(
    name = "BrokenJavaLib",
    srcs = ["NotExists.java"],
    tags = [
        "manual",
        "notap",
    ],
)

# ********************************************

load("//tools/build_rules:build_test.bzl", "build_test")

# test implicit j2cl_library outputs
build_test(
    name = "implicit_outputs",
    targets = [
        ":example_j2cl_lib.js.zip",
        ":libexample_j2cl_lib-src.jar",
    ],
)

# ********************************************

js_library(
    name = "launcher",
    srcs = ["Launcher.js"],
    deps_mgmt = "legacy",
    deps = [
        ":example_j2cl_lib",
        "//javascript/closure:base",
    ],
)

# Depends on library that has dupe src glob.
js_library(
    name = "dupe_launcher",
    srcs = ["Launcher.js"],
    deps_mgmt = "legacy",
    deps = [
        ":example_j2cl_lib",
        ":dupe_j2cl_lib",
        "//javascript/closure:base",
    ],
)

# ********************************************

js_binary(
    name = "J2clJSCompiler",
    compiler = "//javascript/tools/jscompiler:head",
    deps = [":launcher"],
)

# ********************************************

js_module(
    name = "J2clJSCompilerWithModules_js_module",
    srcs = [":launcher"],
)

js_module_binary(
    name = "J2clJSCompilerWithModules",
    compiler = "//javascript/tools/jscompiler:head",
    deps = [":J2clJSCompilerWithModules_js_module"],
)

# ********************************************

js_library(
    name = "J2clAjdJSCompiler_library",
    deps_mgmt = "closure",  # Turns on AJD.
    exports = [":launcher"],
)

js_binary(
    name = "J2clAjdJSCompiler",
    srcs = ["Point.js"],
    compiler = "//javascript/tools/jscompiler:head",
    deps = [":J2clAjdJSCompiler_library"],
)

# ********************************************
# Some zip libraries contain duplicate provides.

js_library(
    name = "J2clDupeAjdJSCompiler_library",
    deps_mgmt = "closure",  # Turns on AJD.
    exports = [":dupe_launcher"],
)

js_binary(
    name = "J2clDupeAjdJSCompiler",
    srcs = ["Point.js"],
    compiler = "//javascript/tools/jscompiler:head",
    deps = [":J2clDupeAjdJSCompiler_library"],
)

# ********************************************

pinto_module(
    name = "J2clPintoPmfEnhancerMss_pinto_module",
    deps_mgmt = "legacy",
    module_id = "J2clPintoPmfEnhancerMss_pinto_module",
    deps = [":launcher"],
)

pinto_processed_module_set(
    name = "J2clPintoPmfEnhancerMss_processed_module_set",
    module_graph_root = "J2clPintoPmfEnhancerMss_pinto_module",
    use_depgraph = False,
    deps = [":J2clPintoPmfEnhancerMss_pinto_module"],
)

filegroup(
    name = "J2clPintoPmfEnhancerMss_filegroup",
    data = [
        "J2clPintoPmfEnhancerMss_warnings_whitelist.txt",
        ":J2clPintoPmfEnhancerMss_pinto_module",
        ":J2clPintoPmfEnhancerMss_processed_module_set",
        "//javascript/externs:common",
    ],
)

mss_js_compiled_module_set_flat_files(
    name = "J2clPintoPmfEnhancerMss",
    app_name = "J2clPintoPmfEnhancerMss",
    compile_modes = ["optimized"],
    config = "J2clPintoPmfEnhancerMss.config",
    languages = ["en"],
    module_sets = ["J2clPintoPmfEnhancerMss_processed_module_set"],
    source_filegroup = ":J2clPintoPmfEnhancerMss_filegroup",
)

# ********************************************

pinto_module(
    name = "J2clAjdPintoPmfEnhancerMss_pinto_module",
    deps_mgmt = "closure",  # Turns on AJD.
    module_id = "J2clAjdPintoPmfEnhancerMss_pinto_module",
    deps = [":launcher"],
)

pinto_processed_module_set(
    name = "J2clAjdPintoPmfEnhancerMss_processed_module_set",
    module_graph_root = "J2clAjdPintoPmfEnhancerMss_pinto_module",
    use_depgraph = False,
    deps = [":J2clAjdPintoPmfEnhancerMss_pinto_module"],
)

filegroup(
    name = "J2clAjdPintoPmfEnhancerMss_filegroup",
    data = [
        "J2clAjdPintoPmfEnhancerMss_warnings_whitelist.txt",
        ":J2clAjdPintoPmfEnhancerMss_pinto_module",
        ":J2clAjdPintoPmfEnhancerMss_processed_module_set",
        "//javascript/externs:common",
    ],
)

mss_js_compiled_module_set_flat_files(
    name = "J2clAjdPintoPmfEnhancerMss",
    app_name = "J2clAjdPintoPmfEnhancerMss",
    compile_modes = ["optimized"],
    config = "J2clAjdPintoPmfEnhancerMss.config",
    languages = ["en"],
    module_sets = ["J2clAjdPintoPmfEnhancerMss_processed_module_set"],
    source_filegroup = ":J2clAjdPintoPmfEnhancerMss_filegroup",
)

# ********************************************
# Some zip libraries contain duplicate provides.

pinto_module(
    name = "J2clDupeAjdPintoPmfEnhancerMss_pinto_module",
    deps_mgmt = "closure",  # Turns on AJD.
    module_id = "J2clDupeAjdPintoPmfEnhancerMss_pinto_module",
    deps = [":dupe_launcher"],
)

pinto_processed_module_set(
    name = "J2clDupeAjdPintoPmfEnhancerMss_processed_module_set",
    enable_j2cl_legacy_mode = True,  # Deal with duplicate files. Enables ZipExpanderPlugin.
    module_graph_root = "J2clDupeAjdPintoPmfEnhancerMss_pinto_module",
    use_depgraph = False,
    deps = [":J2clDupeAjdPintoPmfEnhancerMss_pinto_module"],
)

filegroup(
    name = "J2clDupeAjdPintoPmfEnhancerMss_filegroup",
    data = [
        "J2clDupeAjdPintoPmfEnhancerMss_warnings_whitelist.txt",
        ":J2clDupeAjdPintoPmfEnhancerMss_pinto_module",
        ":J2clDupeAjdPintoPmfEnhancerMss_processed_module_set",
        "//javascript/externs:common",
    ],
)

mss_js_compiled_module_set_flat_files(
    name = "J2clDupeAjdPintoPmfEnhancerMss",
    app_name = "J2clDupeAjdPintoPmfEnhancerMss",
    compile_modes = ["optimized"],
    config = "J2clDupeAjdPintoPmfEnhancerMss.config",
    languages = ["en"],
    module_sets = ["J2clDupeAjdPintoPmfEnhancerMss_processed_module_set"],
    source_filegroup = ":J2clDupeAjdPintoPmfEnhancerMss_filegroup",
)

build_test(
    name = "J2clPintoPmfEnhancerMss_buildtest",
    targets = [
        # This target actually compiles the JS and needs to be referenced by a
        # test to make sure the compile is attempted. Seems like it should be
        # part of the MSS target binary compile... but some magic way it is not.
        ":J2clPintoPmfEnhancerMss_en_optimized_J2clPintoPmfEnhancerMss_processed_module_set",
        ":J2clAjdPintoPmfEnhancerMss",
        ":J2clDupeAjdPintoPmfEnhancerMss",
    ],
)

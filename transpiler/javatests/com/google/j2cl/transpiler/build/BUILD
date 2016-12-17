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

package(default_visibility = ["//:__subpackages__"])

load("//apps/framework/modulesets/builddefs:modulesets.bzl", "assembled_content_app_config", "mss_js_compiled_module_set_flat_files")
load("//javascript/apps/fava/builddefs:fava.bzl", "pinto_processed_module_set")
load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")
load("//build_def:j2cl_util.bzl", "J2CL_UNOPTIMIZED_DEFS")

# Google owns the copyright
licenses(["unencumbered"])

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
    defs = J2CL_UNOPTIMIZED_DEFS,
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
    defs = J2CL_UNOPTIMIZED_DEFS,
    deps = [":J2clJSCompilerWithModules_js_module"],
)

# ********************************************

js_library(
    name = "J2clAjdJSCompiler_library",
    deps_mgmt = "closure",  # Turns on AJD.
    deps = [":launcher"],
)

js_binary(
    name = "J2clAjdJSCompiler",
    srcs = ["Point.js"],
    compiler = "//javascript/tools/jscompiler:head",
    defs = J2CL_UNOPTIMIZED_DEFS,
    deps = [":J2clAjdJSCompiler_library"],
)

# ********************************************
# Some zip libraries contain duplicate provides.

js_library(
    name = "J2clDupeAjdJSCompiler_library",
    deps_mgmt = "closure",  # Turns on AJD.
    deps = [":dupe_launcher"],
)

js_binary(
    name = "J2clDupeAjdJSCompiler",
    srcs = ["Point.js"],
    compiler = "//javascript/tools/jscompiler:head",
    defs = J2CL_UNOPTIMIZED_DEFS,
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
    externs_list = ["//javascript/externs:common"],
    prevalent_modules = "J2clPintoPmfEnhancerMss_pinto_module",
    verify_mod_names = "None",
    verify_mod_names_as_error = False,
    deps = [":J2clPintoPmfEnhancerMss_pinto_module"],
)

assembled_content_app_config(
    name = "J2clPintoPmfEnhancerMss_app_config",
    config_file = "J2clPintoPmfEnhancerMss.config",
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
    config = ":J2clPintoPmfEnhancerMss_app_config",
    languages = ["en"],
    module_sets = ["J2clPintoPmfEnhancerMss_processed_module_set"],
    scs_rowkey_prefix = "/external_content/modulesets/J2clPintoPmfEnhancerMss",
    source_filegroup = ":J2clPintoPmfEnhancerMss_filegroup",
)

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "J2clPintoPmfEnhancerMss_en_optimized_J2clPintoPmfEnhancerMss_processed_module_set_build_test",
    targets = [
        # This target actually compiles the JS and needs to be referenced by a
        # test to make sure the compile is attempted. Seems like it should be
        # part of the MSS target binary compile... but some magic way it is not.
        ":J2clPintoPmfEnhancerMss_en_optimized_J2clPintoPmfEnhancerMss_processed_module_set",
    ],
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
    externs_list = ["//javascript/externs:common"],
    prevalent_modules = "J2clAjdPintoPmfEnhancerMss_pinto_module",
    verify_mod_names = "None",
    verify_mod_names_as_error = False,
    deps = [":J2clAjdPintoPmfEnhancerMss_pinto_module"],
)

assembled_content_app_config(
    name = "J2clAjdPintoPmfEnhancerMss_app_config",
    config_file = "J2clAjdPintoPmfEnhancerMss.config",
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
    config = ":J2clAjdPintoPmfEnhancerMss_app_config",
    languages = ["en"],
    module_sets = ["J2clAjdPintoPmfEnhancerMss_processed_module_set"],
    scs_rowkey_prefix = "/external_content/modulesets/J2clAjdPintoPmfEnhancerMss",
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
    externs_list = ["//javascript/externs:common"],
    prevalent_modules = "J2clDupeAjdPintoPmfEnhancerMss_pinto_module",
    verify_mod_names = "None",
    verify_mod_names_as_error = False,
    deps = [":J2clDupeAjdPintoPmfEnhancerMss_pinto_module"],
)

assembled_content_app_config(
    name = "J2clDupeAjdPintoPmfEnhancerMss_app_config",
    config_file = "J2clDupeAjdPintoPmfEnhancerMss.config",
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
    config = ":J2clDupeAjdPintoPmfEnhancerMss_app_config",
    languages = ["en"],
    module_sets = ["J2clDupeAjdPintoPmfEnhancerMss_processed_module_set"],
    scs_rowkey_prefix = "/external_content/modulesets/J2clDupeAjdPintoPmfEnhancerMss",
    source_filegroup = ":J2clDupeAjdPintoPmfEnhancerMss_filegroup",
)

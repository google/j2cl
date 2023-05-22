"""Macro for generating j2cl_test targets for multiple test files.

Similar to gen_java_tests in third_party/bazel_common/testing/test_defs.bzl,
this macro generates a j2cl_test rule for each file in `srcs` ending in "Test.java" using
the specified deps.

Example usage:

gen_j2cl_tests(
    name = "AllTests",
    srcs = glob(["*.java"]),
)

"""

load(":j2cl_test.bzl", "j2cl_test")
load(":j2cl_library.bzl", "j2cl_library")
load(":j2cl_util.bzl", "get_java_package")

def gen_j2cl_tests(
        name,
        srcs,
        deps = [],
        lib_deps = [],
        test_deps = [],
        plugins = [],
        lib_plugins = [],
        test_plugins = [],
        test_suffix = "",
        tags = [],
        browsers = None,
        generate_build_test = None,
        **kwargs):
    """Generates `j2cl_test` rules for each file in `srcs` ending in "Test.java".

    All other files will be compiled in a supporting `j2cl_library` that is passed
    as a dep to each of the generated `j2cl_test` rules.

    Args:
        name: name of the rule.
        srcs: test sources as well as supporting files.
        deps: dependencies for both the j2cl_lib and all generated j2cl_tests.
        lib_deps: dependencies for the j2cl_lib.
        test_deps: dependencies for the j2cl_tests.
        plugins: plugins to be added to the j2cl_lib and all generated j2cl_tests.
        lib_plugins: plugins to be added to the j2cl_lib.
        test_plugins: plugins to be added to the generated j2cl_tests.
        test_suffix: An optional suffix that can be added to generated test names.
        tags: Tags to add to all tests. In addition, tests are always tagged with
            "gen_j2cl_tests".
        browsers: List of labels; optional; The browsers with which to run the test.
        generate_build_test: Whether the test lib will generate a js_binary and
            build test.
        **kwargs: extra parameters are all passed to the generated j2cl_tests.
    """
    test_files = [src for src in srcs if src.endswith("Test.java")]
    supporting_lib_files = [src for src in srcs if not src.endswith("Test.java")]
    java_package = get_java_package(native.package_name())

    test_deps = deps + test_deps
    if supporting_lib_files:
        supporting_lib_files_name = name + "_j2cl_lib"
        test_deps.append(":" + supporting_lib_files_name)
        j2cl_library(
            name = supporting_lib_files_name,
            deps = deps + lib_deps,
            srcs = supporting_lib_files,
            plugins = lib_plugins + plugins,
            testonly = 1,
            generate_build_test = generate_build_test,
            # Safe here as this is for tests only and there are no downstream users.
            experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = 1,
        )

    for test_file in test_files:
        test_name = test_file[:-len(".java")]
        test_type = test_name.replace("/", ".")
        test_class = java_package + "." + test_type
        j2cl_test(
            name = test_name + test_suffix,
            deps = test_deps,
            srcs = [test_file],
            test_class = test_class,
            plugins = test_plugins + plugins,
            tags = ["gen_j2cl_tests"] + tags,
            browsers = browsers,
            generate_build_test = generate_build_test,
            **kwargs
        )

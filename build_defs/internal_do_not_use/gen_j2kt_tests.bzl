"""Macro for generating j2kt_test targets for multiple test files.

Similar to gen_java_tests in third_party/bazel_common/testing/test_defs.bzl,
this macro generates a j2kt_test rule for each file in `srcs` ending in "Test.java" using
the specified deps.

Example usage:

gen_j2cl_tests(
    name = "AllTests",
    srcs = glob(["*.java"]),
)

"""

load(":j2cl_util.bzl", "get_java_package")
load(":j2kt_library.bzl", "j2kt_native_library")
load(":j2kt_test.bzl", "j2kt_native_test")

def gen_j2kt_native_tests(
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
        **kwargs):
    """Generates `j2kt_native_test` rules for each file in `srcs` ending in "Test.java".

    All other files will be compiled in a supporting `j2kt_native_library` that is passed
    as a dep to each of the generated `j2kt_native_test` rules.

    Args:
        name: name of the rule.
        srcs: test sources as well as supporting files.
        deps: dependencies for both the j2kt_native_lib and all generated j2cl_native_tests.
        lib_deps: dependencies for the j2kt_native_lib.
        test_deps: dependencies for the j2kt_native_tests.
        plugins: plugins to be added to the j2cl_native_lib and all generated j2cl_native_tests.
        lib_plugins: plugins to be added to the j2cl_lib.
        test_plugins: plugins to be added to the generated j2cl_native_tests.
        test_suffix: An optional suffix that can be added to generated test names.
        tags: Tags to add to all tests. In addition, tests are always tagged with
            "gen_j2kt_native_tests".
        **kwargs: extra parameters are all passed to the generated j2kt_native_tests.
    """
    test_files = [src for src in srcs if src.endswith("Test.java")]
    supporting_lib_files = [src for src in srcs if not src.endswith("Test.java")]
    java_package = get_java_package(native.package_name())

    test_deps = deps + test_deps
    if supporting_lib_files:
        supporting_lib_files_name = name + "_j2kt_native_lib"
        test_deps.append(":" + supporting_lib_files_name)
        j2kt_native_library(
            name = supporting_lib_files_name,
            deps = deps + lib_deps,
            srcs = supporting_lib_files,
            plugins = lib_plugins + plugins,
            tags = tags,
            testonly = 1,
        )

    test_targets = []
    for test_file in test_files:
        test_name = test_file[:-len(".java")]
        test_type = test_name.replace("/", ".")
        test_class = java_package + "." + test_type
        test_target_name = test_name + test_suffix
        test_targets.append(":" + test_target_name)
        j2kt_native_test(
            name = test_target_name,
            deps = test_deps,
            srcs = [test_file],
            test_class = test_class,
            plugins = test_plugins + plugins,
            tags = ["gen_j2kt_native_tests"] + tags,
            **kwargs
        )

    native.test_suite(
        name = name,
        tests = test_targets,
    )

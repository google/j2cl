"""Common utilities for creating J2CL and J2WASM test targets

Takes Java source that contains JUnit tests, translates it into
JS or Wasm and packages it into a jsunit_test() rule for testing.


Example use:

# creates jsunit_test(name="MyTest") containing translated JS.

j2cl_test(
    name = "MyTest",
    srcs = ["MyTest.java"],
    deps = [
        ":Bar",  # Directly depends on j2cl_library(name="Bar")
        "//third_party/java/junit:junit-j2cl",
    ],
)

# Similar usage to java_test without sources:
j2cl_library(
    name = "MyTestLib",
    srcs = ["MyTest.java"],
)

j2cl_test(
    name = "MyTest",
    test_class = "mypackage.MyTest"
    runtime_deps = [":MyTestLib"],
)

Note: Unlike java_test tests, MyTestLib have to be in either the direct deps
or direct runtime_deps of j2cl_test for test case transpiliation to work.
This example would fail compilation:

j2cl_library(
    name = "MyTestLib",
    srcs = ["MyTest.java"],
)

j2cl_library(
    name = "MyTestSuiteLib",
    srcs = ["MyTestSuite.java"],
    deps = [":MyTestLib"],
)

j2cl_test(
    name = "MyTest",
    test_class = "mypackage.MyTestSuite"
    runtime_deps = [":MyTestSuiteLib"],
)

To make this example work one needs to add ":MyTestLib" to the runtime_deps of
"MyTest" or to the exports of "MyTestSuiteLib". (b/33477895)

j2wasm_test works the same way where j2cl_library/j2cl_test will be replaced with
j2wasm_library/j2wasm_test counterparts.

j2kt_jvm_test works similarly to j2cl_test and j2wasm_test but it runs test on jvm
rather than on jsunit runner with browsers. For j2kt_jvm_test j2cl_library/j2cl_test
will be replaced with j2kt_jvm_library/j2kt_jvm_test counterparts.
"""

load("@rules_java//java:defs.bzl", "java_test")
load(":j2cl_generate_jsunit_suite.bzl", "j2cl_generate_jsunit_suite")
load(":j2cl_js_common.bzl", "J2CL_TEST_DEFS", "j2cl_web_test")
load(":j2cl_library.bzl", "j2cl_library")
load(":j2cl_rta.bzl", "j2cl_rta")
load(":j2cl_util.bzl", "get_java_package")
load(":j2kt_library.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(":j2wasm_generate_jsunit_suite.bzl", "j2wasm_generate_jsunit_suite")
load(":j2wasm_library.bzl", "j2wasm_library")

_JS_UNIT_TEST_PARAMETERS = [
    "args",
    "compiler",
    "default_browser",
    "deprecation",
    "deps_mgmt",
    "distribs",
    "externs_list",
    "extra_properties",
    "features",
    "instrumentation",
    "licenses",
    "local",
    "plugins",
    "shard_count",
    "size",
    "test_timeout",
    "testonly",
    "timeout",
    "visibility",
]

_STRIP_JSUNIT_PARAMETERS = [
    "args",
    "compiler",
    "default_browser",
    "deps_mgmt",
    "distribs",
    "externs_list",
    "extra_properties",
    "instrumentation",
    "local",
    "plugins",
    "shard_count",
    "size",
    "test_timeout",
    "timeout",
]

_JS_UNIT_DEFAULT_TEST_PARAMETERS = {
    "compiler": "//javascript/tools/jscompiler:head",
    "deps_mgmt": "closure",
}

def _extract_jsunit_parameters(args):
    parameters = {}

    if "defs" in args:
        fail("Usage of defs on j2cl_test is prohibited, use extra_defs instead.")

    for parameter in _JS_UNIT_TEST_PARAMETERS:
        if parameter in args:
            parameters[parameter] = args[parameter]
        elif parameter in _JS_UNIT_DEFAULT_TEST_PARAMETERS:
            parameters[parameter] = _JS_UNIT_DEFAULT_TEST_PARAMETERS[parameter]
    return parameters

def _strip_jsunit_parameters(args):
    parameters = {}
    for parameter in args:
        if not parameter in _STRIP_JSUNIT_PARAMETERS:
            parameters[parameter] = args[parameter]
    return parameters

def _get_test_class(name, build_package, test_class):
    """Infers the name of the test class to be compiled."""
    if name.endswith("-j2cl"):
        name = name[:-5]
    if name.endswith("-j2wasm"):
        name = name[:-7]
    if name.endswith("-j2kt-jvm"):
        name = name[:-9]
    return test_class or get_java_package(build_package) + "." + name

def _verify_attributes(runtime_deps, **kwargs):
    if not kwargs.get("srcs"):
        # Disallow deps without srcs
        if kwargs.get("deps"):
            fail("deps not allowed without srcs; move to runtime_deps?")

        # Need to have runtime deps if there are no sources
        if not runtime_deps:
            fail("without srcs, runtime_deps required")

    # Disallow exports since we use them internally to forward deps to
    # j2cl_generate_jsunit_suite.
    if "exports" in kwargs:
        fail("using exports on j2cl_test is not supported")

# buildifier: disable=function-docstring-args
def j2cl_test_common(
        name,
        runtime_deps = [],
        test_class = None,
        data = [],
        bootstrap_files = [],
        flaky = None,
        compile = 0,
        platform = "CLOSURE",
        optimize_wasm = False,
        wasm_defs = {},
        browsers = None,
        extra_defs = [],
        jvm_flags = [],
        tags = [],
        enable_rta = True,
        **kwargs):
    """Macro for running a JUnit test cross compiled as a web test

       This macro uses the j2cl_test_tranpile macro to transpile tests and feed
       them into a jsunit_test.

    Args:
        platform: the platform on which the test runs
        optimize_wasm: Flag indicating if wasm compilation is optimized or not.
        **kwargs: Additional arguments for web_test rules.
    """

    _verify_attributes(runtime_deps, **kwargs)

    j2cl_parameters = _strip_jsunit_parameters(kwargs)

    # This library serves two purposes:
    #   - Compile srcs files
    #   - Reexport all deps so that they are available to our code generation
    #     within j2cl_generate_jsunit_suite.
    #
    # Reexporting is necessary since generated code refers to test classes
    # which can be either in this library, its deps or its runtime deps.
    exports = (kwargs.get("deps") or []) + runtime_deps
    test_class = _get_test_class(name, native.package_name(), test_class)

    generated_suite_name = name + "_generated_suite"

    deps = []

    if platform == "CLOSURE":
        j2cl_library(
            name = "%s_testlib" % name,
            exports = exports,
            testonly = 1,
            # Safe here as this is for tests only and there are no downstream users.
            experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = 1,
            tags = tags,
            **j2cl_parameters
        )

        # Trigger our code generation
        j2cl_generate_jsunit_suite(
            name = generated_suite_name,
            test_class = test_class,
            deps = [":%s_testlib" % name],
            tags = tags,
        )

        deps = [
            # We add this direct dependency to prevent AJD from pruning _js_srcs.
            # This is for bootsrap sources which need to be passed as _js_srcs in
            # j2cl_test for the compiled mode to pick it up (otherwise dropped
            # in jsunit_test if user provided only in bootstrap_files).
            ":%s_testlib" % name,
            ":%s_lib" % generated_suite_name,
            Label("//build_defs/internal_do_not_use:closure_testsuite"),
            Label("//build_defs/internal_do_not_use:closure_testcase"),
            Label("//build_defs/internal_do_not_use:internal_parametrized_test_suite"),
        ]

    elif platform == "WASM":
        j2wasm_library(
            name = "%s_testlib" % name,
            exports = exports,
            testonly = 1,
            tags = tags,
            **j2cl_parameters
        )

        # Trigger our code generation
        j2wasm_generate_jsunit_suite(
            name = generated_suite_name,
            test_class = test_class,
            deps = [":%s_testlib" % name],
            tags = tags,
            optimize = optimize_wasm,
            defines = wasm_defs,
            exec_properties = kwargs.get("exec_properties") or {},
        )

        deps = [
            ":%s_dep" % generated_suite_name,
            Label("//build_defs/internal_do_not_use:closure_testsuite"),
            Label("//build_defs/internal_do_not_use:closure_testcase"),
        ]

        # Open-source currently needs the explicit data dependency to wasm target.
        data = data + [
            ":%s_dep" % generated_suite_name,
        ]

    else:
        fail("Unknown platform: " + platform)

    jsunit_parameters = _extract_jsunit_parameters(kwargs)

    defs = J2CL_TEST_DEFS + extra_defs
    jvm_flags = jvm_flags + ["-DstacktraceDeobfuscation=true"]
    bootstrap_files = bootstrap_files + [
        # Taken from jsunit_test_suites.
        "//testing/web/js/browser_services:onerror_at_boot.js",
        "//testing/web/js/browser_services:console_poster.js",
    ]

    if compile:
        defs.append("--define=goog.ENABLE_DEBUG_LOADER=true")
    else:
        # enforce bundled mode since the debug loader is disabled
        jvm_flags.append("-Djsrunner.net.useJsBundles=true")

    jsunit_parameters.update({
        "jvm_flags": jvm_flags,
        "defs": defs,
        "compile": compile,
        "bootstrap_files": bootstrap_files,
    })

    j2cl_web_test(
        name = name,
        src = ":" + generated_suite_name,
        deps = deps,
        browsers = browsers,
        data = data,
        tags = tags,
        flaky = flaky,
        test_class = test_class,
        **jsunit_parameters
    )

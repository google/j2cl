"""j2cl_test build macro

Takes Java source that contains JUnit tests, translates it into Closure style
JS and packages it into a jsunit_test() rule for testing.


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

# Similiar usage to java_test without sources:
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
"""

load("//testing/web/build_defs:web.bzl", "web_test")
load("//testing/web/build_defs/js:js.bzl", "jsunit_test")
load("//build_def:j2cl_library.bzl", "j2cl_library")
load(
    "//build_def:j2cl_generate_jsunit_suite.bzl",
    "j2cl_generate_jsunit_suite",
)
load(
    "//build_def:j2cl_util.bzl",
    "J2CL_TEST_DEFS",
    "get_java_package",
)

_JS_UNIT_TEST_PARAMETERS = [
    "args",
    "bootstrap_files",
    "compile",
    "compiler",
    "data",
    "defs",
    "deprecation",
    "deps_mgmt",
    "distribs",
    "externs_list",
    "extra_properties",
    "features",
    "flaky",
    "instrumentation",
    "jvm_flags",
    "licenses",
    "local",
    "plugins",
    "shard_count",
    "size",
    "tags",
    "test_timeout",
    "testonly",
    "timeout",
    "visibility",
]

_STRIP_JSUNIT_PARAMETERS = [
    "args",
    "bootstrap_files",
    "compile",
    "compiler",
    "data",
    "defs",
    "deps_mgmt",
    "distribs",
    "externs_list",
    "extra_defs",
    "extra_properties",
    "flaky",
    "instrumentation",
    "jvm_flags",
    "local",
    "plugins",
    "shard_count",
    "size",
    "test_timeout",
    "timeout",
]

_JS_UNIT_DEFAULT_TEST_PARAMETERS = {
    "compiler": "//javascript/tools/jscompiler:head",
    "data": [],
    "deps_mgmt": "closure",
    "externs_list": ["//javascript/externs:common"],
    "jvm_flags": [
        "-DstacktraceDeobfuscation=true",
    ],
}

def _extract_jsunit_parameters(args):
    parameters = {}
    parameters["defs"] = J2CL_TEST_DEFS + args.get("extra_defs", [])

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
    return test_class or get_java_package(build_package) + "." + name

def _verify_attributes(runtime_deps, **kwargs):
    if not kwargs.get("srcs"):
        # Disallow deps without srcs
        if kwargs.get("deps"):
            fail("deps not allowed without srcs; move to runtime_deps?")

        # Disallow _js_deps without srcs
        if kwargs.get("_js_deps"):
            fail("_js_deps not allowed without srcs")

        # Need to have runtime deps if there are no sources
        if not runtime_deps:
            fail("without srcs, runtime_deps required")

    # Disallow exports since we use them internally to forward deps to
    # j2cl_generate_jsunit_suite.
    if "exports" in kwargs:
        fail("using exports on j2cl_test is not supported")

def j2cl_test(
        name,
        runtime_deps = [],
        test_class = None,
        tags = [],
        **kwargs):
    """Macro for running a JUnit test cross compiled as a web test

       This macro uses the j2cl_test_tranpile macro to transpile tests and feed
       them into a jsunit_test.
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
    j2cl_library(
        name = "%s_lib" % name,
        exports = exports,
        testonly = 1,
        tags = tags,
        **j2cl_parameters
    )

    test_class = _get_test_class(name, native.package_name(), test_class)

    # Trigger our code generation
    j2cl_generate_jsunit_suite(
        name = name + "_generated_suite",
        test_class = test_class,
        deps = [":%s_lib" % name],
        tags = tags,
    )

    jsunit_parameters = _extract_jsunit_parameters(kwargs)

    # enforce bundled mode since the debug loader is disabled
    jsunit_parameters["jvm_flags"] = jsunit_parameters["jvm_flags"] + ["-Djsrunner.net.useJsBundles=true"]

    # Define a jsunit_test:
    #   - sources is the zip coming from code gen
    #   - deps contains the j2cl_library from our code gen
    wrapped_test_name = "%s_debug" % name
    jsunit_test(
        name = wrapped_test_name,
        srcs = [":%s_generated_suite.js.zip" % name],
        deps = [
            # We add this direct dependency to prevent AJD from pruning _js_srcs.
            # This is for bootsrap sources which need to be passed as _js_srcs in
            # j2cl_test for the compiled mode to pick it up (otherwise dropped
            # in jsunit_test if user provided only in bootstrap_files).
            ":%s_lib" % name,
            ":%s_generated_suite_lib" % name,
            "//javascript/closure/testing:testsuite",
        ],
        tags = depset(tags + ["manual", "notap"]),
        **jsunit_parameters
    )

    web_test(
        name = name,
        browser = "//testing/web/browsers:chrome-linux",
        config = "//testing/web/configs:default_noproxy",
        tags = tags,
        test = ":%s" % wrapped_test_name,
        flaky = jsunit_parameters.get("flaky"),
    )

"""j2wasm_generate_jsunit_suite build macro

Takes Java source that contains JUnit tests and translates it into a goog.testing.testSuite.


Example using a jsunit_test:

j2wasm_library(
    name = "mytests",
    ...
)

j2wasm_generate_jsunit_suite(
    name = "CodeGen",
    deps = [":mytests"],
    test_class = "mypackage.MyTest",
)

jsunit_test(
  name = "test",
  srcs = [":CodeGen.js.zip"],
  deps = [":CodeGen"],
  ...
)

"""

load(":generate_test_input.bzl", "generate_test_input")
load(":j2wasm_application.bzl", "j2wasm_application")
load(":j2wasm_library.bzl", "j2wasm_library")

# buildifier: disable=function-docstring-args
def j2wasm_generate_jsunit_suite(
        name,
        test_class,
        deps,
        tags = [],
        optimize = False,
        defines = {}):
    """Macro for cross compiling a JUnit Suite to .wasm file.

    Args:
        optimize: A flag indicating if wasm compilation is optimized or not.
    """

    test_input = generate_test_input(name, test_class)

    # This target triggers our Java annotation processor and generates the
    # plumbing between a jsunit_test and transpiled JUnit tests
    # Its outputs are:
    #  - test_summary.json that lists all jsunit test suites
    #  - A .testsuite file for every JUnit test containing a jsunit tests suite
    #  - A Java class that contains the bridge between the jsunit test
    #        suite and the JUnit test. This is being used from the .testsuite
    #        file.
    #
    # We separated this out into a separate target so we can have dependencies
    # here that users might not have in their tests.
    # We need the extra dep here on user provided dependencies since our generated
    # code refers to user written code (test cases).
    # Note that test suites are generated with .testsuite extension to avoid
    # j2cl_library automically including them as source.
    j2wasm_library(
        name = name + "_lib",
        srcs = [test_input],
        deps = deps + [
            Label("//build_defs/internal_do_not_use:internal_junit_annotations-j2wasm"),
            Label("//build_defs/internal_do_not_use:internal_junit_runtime-j2wasm"),
            Label("//build_defs/internal_do_not_use:closure_testcase"),
        ],
        testonly = 1,
        javacopts = ["-AtestPlatform=WASM"],
        tags = tags,
    )

    # The Java annotation processor on the above target generates jsunit suites
    # (.testsuite files), but the same jar file also contains unrelated things
    out_jar = ":lib" + name + "_lib.jar"

    test_defines = dict({
        "J2WASM_DEBUG": "TRUE",
        "jre.checkedMode": "ENABLED",
        "jre.logging.logLevel": "ALL",
        "jre.logging.simpleConsoleHandler": "ENABLED",
    })
    test_defines.update(defines)

    j2wasm_application_name = name + "_j2wasm_application"

    j2wasm_application(
        name = j2wasm_application_name,
        deps = [":" + name + "_lib"],
        defines = test_defines,
        enable_debug_info = True,
        entry_points = [
            ".*_Adapter#test.*",
            ".*_Adapter#setUp.*",
            ".*_Adapter#tearDown.*",
        ],
        testonly = 1,
        tags = tags + ["manual", "notap"],
    )

    # Re-expose the target as "_dep" for test infra to depend on.
    wasm_target = j2wasm_application_name + ("" if optimize else "_dev")
    native.alias(name = name + "_dep", actual = wasm_target)

    # This genrule takes the jar file as input and creates
    # a new zip file that only contains the generated javascript (.testsuite
    # renamed to .js) and the test_summary.json file.
    # This is the format that jsunit_test will later expect.
    # extracting files from jar (output js zip can include all the required
    # files.)

    wasm_path = "/" + native.package_name() + "/" + wasm_target + ".wasm"

    wasm_module_name = wasm_target.replace("-", "_") + ".j2wasm"
    processed_wasm_path = wasm_path.replace("/", "\\/")

    native.genrule(
        name = name,
        srcs = [out_jar],
        outs = [name + ".js.zip"],
        cmd = "\n".join([
            "TMP=$$(mktemp -d)",
            "WD=$$(pwd)",
            "unzip -q $(location %s) *.testsuite *.json -d $$TMP" % out_jar,
            "cd $$TMP",
            "for f in $$(find . -name *.testsuite); do" +
            " sed -i -e 's/REPLACEMENT_MODULE_NAME_PLACEHOLDER/%s/' $$f ;" % wasm_module_name +
            " sed -i -e 's/REPLACEMENT_BUILD_PATH_PLACEHOLDER/%s/' $$f ;" % processed_wasm_path +
            " mv $$f $${f/.testsuite/.js}; done",
            "zip -q -r $$WD/$@ .",
            "rm -rf $$TMP",
        ]),
        testonly = 1,
        tags = tags + ["manual", "notap"],
    )

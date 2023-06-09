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

load(":j2wasm_library.bzl", "j2wasm_library")
load(":j2wasm_application.bzl", "j2wasm_application")
load(":generate_test_input.bzl", "generate_test_input")

# buildifier: disable=function-docstring-args
def j2wasm_generate_jsunit_suite(
        name,
        test_class,
        deps,
        tags = [],
        optimize = False,
        defines = {},
        exec_properties = {}):
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
            "//build_defs/internal_do_not_use:internal_junit_runtime-j2wasm",
        ],
        javacopts = ["-AtestPlatform=WASM"],
        testonly = 1,
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
        entry_points = [
            ".*_Adapter#test.*",
            ".*_Adapter#setUp.*",
            ".*_Adapter#tearDown.*",
        ],
        testonly = 1,
        exec_properties = exec_properties,
    )

    # This genrule takes the jar file as input and creates
    # a new zip file that only contains the generated javascript (.testsuite
    # renamed to .js) and the test_summary.json file.
    # This is the format that jsunit_test will later expect.
    # extracting files from jar (output js zip can include all the required
    # files.)

    wasm_optimized_suffix = "" if optimize else "_dev"
    wasm_path = "/google3/" + native.package_name() + "/" + j2wasm_application_name + wasm_optimized_suffix + ".wasm"
    wasm_module_name = j2wasm_application_name.replace("-", "_") + ".j2wasm"
    processed_wasm_path = wasm_path.replace("/", "\\/")

    native.genrule(
        name = name + "_transpile_gen",
        srcs = [out_jar],
        outs = [name + ".js.zip"],
        cmd = "\n".join([
            "unzip -q $(location %s) *.testsuite *.json -d zip_out/" % out_jar,
            "cd zip_out/",
            "for f in $$(find . -name *.testsuite); do" +
            " sed -i -e 's/REPLACEMENT_MODULE_NAME_PLACEHOLDER/%s/' $$f ;" % wasm_module_name +
            " sed -i -e 's/REPLACEMENT_BUILD_PATH_PLACEHOLDER/%s/' $$f ;" % processed_wasm_path +
            " mv $$f $${f/.testsuite/.js}; done",
            "zip -q -r ../$@ .",
        ]),
        testonly = 1,
        tags = ["manual", "notap"],
    )

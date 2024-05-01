"""j2cl_generate_jsunit_suite build macro

Takes Java source that contains JUnit tests and translates it into a goog.testing.testSuite.


Example using a jsunit_test:

j2cl_library(
    name = "mytests",
    ...
)

j2cl_generate_jsunit_suite(
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
load(":j2cl_library.bzl", "j2cl_library")

# buildifier: disable=function-docstring-args
def j2cl_generate_jsunit_suite(name, test_class, deps, tags = []):
    """Macro for cross compiling a JUnit Suite to JavaScript testSuite"""

    test_input = generate_test_input(name, test_class)

    # This target triggers our Java annotation processor and generates the
    # plumbing between a jsunit_test and transpiled JUnit tests
    # It's outputs are:
    #  - test_summary.json that lists all jsunit test suites
    #  - A .testsuite file for every JUnit test containing a jsunit tests suite
    #  - A Java class (JsType) that contains the bridge between the jsunit test
    #        suite and the JUnit test. This is being used from the .testsuite
    #        file.
    #
    # We separated this out into a separate target so we can have dependencies
    # here that users might not have in their tests (e.g. jsinterop annotations).
    # We need the extra dep here on user provided dependencies since our generated
    # code refers to user written code (test cases).
    # Note that test suites are generated with .testsuite extension to avoid
    # j2cl_library automically including them as source.
    j2cl_library(
        name = name + "_lib",
        srcs = [test_input],
        deps = deps + [
            Label("//build_defs/internal_do_not_use:internal_junit_annotations"),
            Label("//build_defs/internal_do_not_use:internal_junit_runtime"),
            Label("//build_defs/internal_do_not_use:closure_testcase"),
        ],
        testonly = 1,
        javacopts = [
            "-AtestPlatform=CLOSURE",
            # Disable error prone checks since this is a generated code.
            "-Xep:PackageLocation:OFF",
        ],
        tags = tags,
        generate_build_test = False,
    )

    # The Java annotation processor on the above target generates jsunit suites
    # (.testsuite files), but the same jar file also contains unrelated things
    # (e.g. class files), this genrules takes the jar file as input and creates
    # a new zip file that only contains the generated javascript (.testsuite
    # renamed to .js) and the test_summary.json file.
    # This is the format that jsunit_test will later expect.
    # TODO(goktug): use j2cl_library directly from jsunit_test instead of
    # extracting files from jar (output js zip can include all the required
    # files.)
    out_jar = ":lib" + name + "_lib.jar"
    native.genrule(
        name = name,
        outs = [name + ".js.zip"],
        cmd = "\n".join([
            "unzip -q $(location %s) *.testsuite *.json -d zip_out/" % out_jar,
            "cd zip_out/",
            "for f in $$(find . -name *.testsuite); do mv $$f $${f/.testsuite/.js}; done",
            "zip -q -r ../$@ .",
        ]),
        testonly = 1,
        tags = ["manual", "notap"],
        tools = [out_jar],
    )

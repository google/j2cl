"""j2cl_java_test macro

A build macro that uses j2cl_java_library to cross compile Java to JavaScript
and emits closure test suites for all JUnit Tests.
Note: The source attribute must only glob actual JUnit4 test cases. If you need
extra Java files for your tests use a j2cl_java_library and add it as a
dependency.

Here is an example usage:

load("/third_party/java_src/j2cl/build_def/j2cl_java_test", "j2cl_java_test")

j2cl_java_library(
    name = "my_library",
    srcs = ["MyNonTestSource.java"],
)

j2cl_java_test(
    name = "MyTest",
    srcs = ["MyTest.java"],
    deps = [
        ":my_library",
        "//third_party/java/junit",
    ],
)

"""

load("/third_party/java_src/j2cl/build_def/j2cl_java_library", "j2cl_java_library")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_root")

def j2cl_java_test(**kwargs):
  """Macro for running a JUnit test cross compiled as a web test

     This macro uses the j2cl_java_library macro to transpile test and
     runs an APT on the java_library which outputs JavaScript files for all
     JUnit Test cases.
     This JavaScript is extracted from the jar with a genrule and then fed into
     a js_unit / web_test.
  """
  java_root = get_java_root(PACKAGE_NAME)
  base_name = kwargs["name"]
  kwargs["testonly"] = 1

  # Add our APT to java plugins.
  if not "plugins" in kwargs:
    kwargs["plugins"] = []
  kwargs["plugins"] = kwargs["plugins"] + ["//:junit_processor"]

  # JavaScript file names that will be produced by our APT
  js_names = []

  if not "srcs" in kwargs:
    fail("No srcs defined in rule")
  for src in kwargs["srcs"]:
    js_names += [src[:-len(".java")] + "_generated.js"]

  # path to the jar produced by the java_library rule defined in
  # j2cl_java_library
  out_jar = "blaze-out/host/bin/" + PACKAGE_NAME + "/lib" + base_name + ".jar"

  j2cl_java_library(**kwargs)

  native.genrule(
    name = base_name + "_extract_js",
    srcs = [":" + base_name],
    outs = js_names,
    cmd = "$(location //third_party/unzip:unzip) -q -d $(GENDIR)/"
        + java_root + " " + out_jar + "",
    executable = 1,
    testonly = 1,
    tools = [
        "lib" + base_name + ".jar",
        "//third_party/unzip",
    ],
  )

  # TODO(dankurka): Add support for different browsers
  # TODO(dankurka): Investigate better setup with jsunit_test / web_test
  native.jsunit_test(
      name = base_name + "_js_test",
      srcs = [":" + base_name + "_extract_js",],
      compile = 1,
      compiler = "//javascript/tools/jscompiler:head",
      defs = [
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--jscomp_off=nonStandardJsDocs",
          "--common_js_module_path_prefix=" + java_root,
          "--jscomp_off=checkTypes",
          "--jscomp_off=undefinedVars",
          "--export_test_functions=true",
          "--property_renaming=OFF",
          "--strict",
          "--variable_renaming=OFF",
          "--pretty_print",
      ],
      externs_list = ["//javascript/externs:common"],
      deps = [
          ":" + base_name + "_js_library",
          "//javascript/closure/testing:testsuite",
      ],
      jvm_flags = [
         "-Dcom.google.testing.selenium.browser=CHROME_LINUX"
     ],
     data = ["//testing/matrix/nativebrowsers/chrome:stable_data",],
  )

  native.web_test(
    name = base_name + "_web_test",
    test = base_name + "_js_test",
    browser = "chrome-linux",
  )

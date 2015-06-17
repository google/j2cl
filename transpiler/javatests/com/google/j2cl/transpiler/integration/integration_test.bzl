"""integration_test build macro.

A build macro that turns Java files into an optimized JS target and a JS
test target.

The set of Java files must have a Main class with a main() function.


Example usage:

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "foobar",
    srcs = glob(["*.java"]),
)
"""

load(
    "/third_party/java_src/j2cl/build_def/j2cl_java_library",
    "j2cl_java_library",
)
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_root")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_package")

# TODO: source these flags from the authoritative location once they're
#       accessible from .bzl
CLOSURE_COMPILER_FLAGS_FULL_TYPED = [
    "--aggressive_var_check_level=WARNING",
    "--check_global_names_level=ERROR",
    "--check_provides=WARNING",
    "--closure_pass",
    "--collapse_properties",
    "--compute_function_side_effects=true",
    "--devirtualize_prototype_methods",
    "--inline_variables",
    "--jscomp_warning=deprecated",
    "--jscomp_warning=missingProperties",
    "--jscomp_warning=visibility",
    "--property_renaming=ALL_UNQUOTED",
    "--remove_unused_prototype_props",
    "--remove_unused_prototype_props_in_externs",
    "--smart_name_removal",
    "--variable_renaming=ALL",
    "--ambiguate_properties",
    "--disambiguate_properties",
]

def integration_test(name, srcs, show_debug_cmd=False, deps=[]):
  """Macro that turns Java files into integration test targets."""
  # figure out the current location
  java_root_path = get_java_root(PACKAGE_NAME)
  java_package = get_java_package(PACKAGE_NAME)

  # translate Java to JS
  j2cl_java_library(
      name = name,
      srcs = srcs,
      show_debug_cmd = show_debug_cmd,
      deps = deps
  )

  # blaze build :optimized_js
  opt_harness = """
      goog.module('gen.opt.Harness');
      var Main = goog.require('gen.%s.MainModule').Main;
      Main.m_main__arrayOf_java_lang_String();
  """ % java_package
  native.genrule(
      name = "opt_harness_generator",
      outs = ["OptHarness.js"],
      cmd = "echo \"%s\" > $@" % opt_harness,
      executable = 1,
  )
  native.js_binary(
      name = "optimized_js",
      srcs = ["OptHarness.js"],
      defs = CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--remove_dead_assignments",
          "--remove_dead_code",
          "--remove_unused_local_vars=ON",
          "--remove_unused_vars",
      ],
      externs_list = ["//javascript/externs:common"],
      deps = [":" + name + "_js_library"],
  )

  # blaze test :readable_js
  test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();
      var testSuite = goog.require('goog.testing.testSuite');
      var Main = goog.require('gen.%s.MainModule').Main;
      testSuite({
        test_Main: function() {
          Main.m_main__arrayOf_java_lang_String();
        }
      });
  """ % java_package
  native.genrule(
      name = "test_harness_generator",
      outs = ["TestHarness.js"],
      cmd = "echo \"%s\" > $@" % test_harness,
  )
  # TODO: use google_js_test when it supports ES6
  native.jsunit_test(
      name = name + "_test_spec",
      srcs = ["TestHarness.js"],
      compile = 1,
      defs = [
          "--export_test_functions=true",
          "--jscomp_off=checkTypes",
          "--jscomp_off=undefinedVars",
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--property_renaming=OFF",
          "--pretty_print",
          "--strict",
          "--variable_renaming=OFF",
      ],
      externs_list = ["//javascript/externs:common"],
      deps = [
          ":" + name + "_js_library",
          "//javascript/closure/testing:testsuite",
      ],
      jvm_flags = [
         "-Dcom.google.testing.selenium.browser=CHROME_LINUX"
     ],
     data = ["//testing/matrix/nativebrowsers/chrome:stable_data",],
  )
  native.web_test(
    name = "readable_js",
    test = name + "_test_spec",
    browser = "chrome-linux",
  )

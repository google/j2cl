"""integration_test build macro.

A build macro that turns Java files into an optimized JS target and a JS
test target.

The set of Java files must have a Main class with a main() function.


Example usage:

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
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
    "--ambiguate_properties",
    "--disambiguate_properties",
    "--remove_unused_constructor_properties=ON",
]


def integration_test(name, srcs, deps=[]):
  """Macro that turns Java files into integration test targets."""
  # figure out the current location
  java_root_path = get_java_root(PACKAGE_NAME)
  java_package = get_java_package(PACKAGE_NAME)

  # translate Java to JS
  j2cl_java_library(
      name=name,
      srcs=srcs,
      deps=deps,
      javacopts=[
        "-source 8",
        "-target 8"
      ]
  )

  # blaze build :optimized_js
  opt_harness = """
      goog.module('gen.opt.Harness');
      var Main = goog.require('gen.%s.MainModule').Main;
      Main.m_main__arrayOf_java_lang_String([]);
  """ % java_package
  native.genrule(
      name="opt_harness_generator",
      outs=["OptHarness.js"],
      cmd="echo \"%s\" > $@" % opt_harness,
      executable=1,
  )
  native.js_binary(
      name="optimized_js",
      srcs=["OptHarness.js"],
      defs=CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--define=ASSERTIONS_ENABLED_=true",
          "--remove_dead_assignments",
          "--remove_dead_code",
          "--remove_unused_local_vars=ON",
          "--remove_unused_vars",
          "--variable_renaming=ALL",
      ],
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=[":" + name + "_js_library"],
  )
  # For constructing readable optimized diffs.
  native.js_binary(
      name="readable_optimized_js",
      srcs=["OptHarness.js"],
      defs=CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--define=ASSERTIONS_ENABLED_=true",
          "--remove_dead_assignments",
          "--remove_dead_code",
          "--remove_unused_local_vars=ON",
          "--remove_unused_vars",
          "--pretty_print",
          "--property_renaming=OFF",
          "--variable_renaming=OFF",
      ],
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=[":" + name + "_js_library"],
  )
  # For constructing readable unoptimized diffs.
  native.js_binary(
      name="readable_unoptimized_js",
      srcs=["OptHarness.js"],
      defs=[
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--define=ASSERTIONS_ENABLED_=true",
          "--pretty_print",
      ],
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=[":" + name + "_js_library"],
  )

  # For constructing GWT transpiled output.
  gwt_harness = """
      package %s;
      import com.google.gwt.core.client.EntryPoint;
      public class MainEntryPoint implements EntryPoint {
        @Override
        public void onModuleLoad() {
          Main.main(new String[] {});
        }
      }
  """ % java_package
  native.genrule(
      name="gwt_harness_generator",
      outs=["MainEntryPoint.java"],
      cmd="echo \"%s\" > $@" % gwt_harness,
      executable=1,
  )
  native.gwt_module(
      name="gwt_module",
      srcs=srcs + ["MainEntryPoint.java"],
      deps=deps,
      entry_points=[java_package + ".MainEntryPoint"],
      javacopts=[
        "-source 8",
        "-target 8"
      ]
  )
  native.gwt_application(
      name="readable_gwt_application",
      compiler_opts=[
          "-optimize 0",
          "-style PRETTY",
          "-setProperty user.agent=safari",
          "-ea",
      ],
      module_target=":gwt_module",
      tags=["manual"],
  )

  # blaze test :uncompiled_test
  # blaze test :compiled_test
  test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();
      // Enable assertions in uncompiled test mode.
      window.ASSERTIONS_ENABLED_ = true;
      var testSuite = goog.require('goog.testing.testSuite');
      var Main = goog.require('gen.%s.MainModule').Main;
      testSuite({
        test_Main: function() {
          Main.m_main__arrayOf_java_lang_String([]);
        }
      });
  """ % java_package
  native.genrule(
      name="test_harness_generator",
      outs=["TestHarness.js"],
      cmd="echo \"%s\" > $@" % test_harness,
  )
  native.jsunit_test(
      name="uncompiled_test",
      srcs=["TestHarness.js"],
      compile=0,
      deps=[
          ":" + name + "_js_library",
          "//javascript/closure/testing:testsuite",
      ],
      deps_mgmt="closure",
      externs_list=["//javascript/externs:common"],
      jvm_flags=["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
      data=["//testing/matrix/nativebrowsers/chrome:stable_data",],
  )

  native.jsunit_test(
      name="compiled_test",
      srcs=["TestHarness.js"],
      compile=1,
      compiler="//javascript/tools/jscompiler:head",
      defs=CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--export_test_functions=true",
          "--jscomp_off=checkTypes",
          "--jscomp_off=undefinedVars",
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--define=ASSERTIONS_ENABLED_=true",
          "--property_renaming=OFF",
          "--pretty_print",
          "--strict",
          "--variable_renaming=OFF",
      ],
      deps=[
          ":" + name + "_js_library",
          "//javascript/closure/testing:testsuite",
      ],
      deps_mgmt="closure",
      externs_list=["//javascript/externs:common"],
      jvm_flags=["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
      data=["//testing/matrix/nativebrowsers/chrome:stable_data",],
  )

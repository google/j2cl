"""integration_test build macro

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

load("/javascript/closure/builddefs", "CLOSURE_COMPILER_FLAGS_FULL_TYPED")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_package")
load("/tools/build_defs/label/def", "absolute_label")

# Copy the Closure flags but remove --variable_renaming=ALL since it interferes
# with tests and can't be turned off.
_CLOSURE_COMPILER_FLAGS_FULL_TYPED = [
    flag
    for flag in CLOSURE_COMPILER_FLAGS_FULL_TYPED
    if flag != "--variable_renaming=ALL"
]

def integration_test(
    name, srcs=[], deps=[], defs=[], native_srcs=[],
    native_srcs_pkg="CONVENTION", js_deps=[], closure_defines=dict(),
    generate_build_test=None):
  """Macro that turns Java files into integration test targets.

  deps are Labels of j2cl_library() rules. NOT labels of
  java_library() rules.
  """
  # figure out the current location
  java_package = get_java_package(PACKAGE_NAME)

  deps = [absolute_label(dep) for dep in deps]

  closure_defines = dict(closure_defines) # make a copy to ensure mutability

  # Turn on assertions since the integration tests rely on them.
  if not "ASSERTIONS_ENABLED_" in closure_defines:
    closure_defines["ASSERTIONS_ENABLED_"] = "true"
  # Since integration tests are used for optimized size tracking, set array
  # behavior to the mode with the smallest output size which is what we expect
  # will also be used for customer application production releases. If some
  # particular test needs one of these on they can override with
  # closure_defines.
  if not "ARRAY_CHECK_BOUNDS_" in closure_defines:
    closure_defines["ARRAY_CHECK_BOUNDS_"] = "false"
  if not "ARRAY_CHECK_TYPES_" in closure_defines:
    closure_defines["ARRAY_CHECK_TYPES_"] = "false"

  define_flags = []
  for def_name, value in closure_defines.items():
    define_flags.append("--define={0}={1}".format(def_name, value))
  defs = defs + define_flags

  srcs_lib_dep = []
  if srcs:
    srcs_lib_dep = [":" + name]
    # translate Java to JS
    j2cl_library(
        name=name,
        srcs=srcs,
        generate_build_test=generate_build_test,
        deps=deps,
        javacopts=[
            "-source 8",
            "-target 8"
        ],
        native_srcs=native_srcs,
        native_srcs_pkg=native_srcs_pkg,
    )

  # blaze build :optimized_js
  opt_harness = """
      goog.module('gen.opt.Harness');
      var Main = goog.require('%s.Main');
      Main.m_main__arrayOf_java_lang_String([]);
  """ % java_package
  native.genrule(
      name="opt_harness_generator",
      outs=["OptHarness.js"],
      cmd="echo \"%s\" > $@" % opt_harness,
      executable=1,
  )

  # NOTE: --closure_entry_point is not used (to avoid hiding leaks in the size
  #       report).
  # NOTE: --norewrite_polyfills *is* used so that size tracking only focuses on
  #       size issues that are actionable outside of JSCompiler or are expected
  #       to eventually be addressed inside of JSCompiler.
  native.js_binary(
      name="optimized_js",
      srcs=["OptHarness.js"],
      defs=_CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--j2cl_pass",
          "--language_in=ECMASCRIPT6_STRICT",
          "--language_out=ECMASCRIPT5",
          "--remove_dead_assignments",
          "--remove_dead_code",
          "--remove_unused_local_vars=ON",
          "--remove_unused_vars",
          "--norewrite_polyfills",
          "--variable_renaming=ALL",
          "--jscomp_off=lateProvide",
      ] + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=js_deps + srcs_lib_dep,
  )
  # For constructing readable optimized diffs.
  native.js_binary(
      name="readable_optimized_js",
      srcs=["OptHarness.js"],
      defs=_CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--j2cl_pass",
          "--language_in=ECMASCRIPT6_STRICT",
          "--language_out=ECMASCRIPT5",
          "--remove_dead_assignments",
          "--remove_dead_code",
          "--remove_unused_local_vars=ON",
          "--remove_unused_vars",
          "--norewrite_polyfills",
          "--pretty_print",
          "--property_renaming=OFF",
          "--variable_renaming=OFF",
          "--jscomp_off=lateProvide",
      ] + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=js_deps + srcs_lib_dep,
  )
  # For constructing readable unoptimized diffs.
  native.js_binary(
      name="readable_unoptimized_js",
      srcs=["OptHarness.js"],
      defs=[
          "--j2cl_pass",
          "--language_in=ECMASCRIPT6_STRICT",
          "--language_out=ECMASCRIPT5",
          "--pretty_print",
          "--property_renaming=OFF",
          "--variable_renaming=OFF",
          "--jscomp_off=lateProvide",
      ] + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=js_deps + srcs_lib_dep,
  )

  # For constructing GWT transpiled output.
  srcjars = [src for src in srcs if ".srcjar" in src]
  if srcs and not srcjars:
    # Only provide a GWT target if there are no srcjars since gwt_module can't
    # handle them directly.
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
    java_library_deps = [dep + "_java_library" for dep in deps]
    native.gwt_module(
        name="gwt_module",
        srcs=srcs + ["MainEntryPoint.java"],
        restricted_to = ["//buildenv/j2cl:j2cl_compilation"],  # SPECIAL CASE. Normal code should not do this.
        deps=java_library_deps,
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
            "-generateJsInteropExports",
            "-ea",
        ],
        shard_count=1,
        module_target=":gwt_module",
        tags=["manual"],
    )

  test_harness_defines = ""
  for def_name, value in closure_defines.items():
    test_harness_defines += "window.{0} = {1};\n".format(def_name, value)

  # blaze test :uncompiled_test
  # blaze test :compiled_test
  test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();

      // Closure defines.
      %s

      var testSuite = goog.require('goog.testing.testSuite');
      var Main = goog.require('%s.Main');
      testSuite({
        test_Main: function() {
          Main.m_main__arrayOf_java_lang_String([]);
        }
      });
  """ % (test_harness_defines, java_package)
  native.genrule(
      name="test_harness_generator",
      outs=["TestHarness.js"],
      cmd="echo \"%s\" > $@" % test_harness,
  )

  native.jsunit_test(
      name="uncompiled_test",
      srcs=["TestHarness.js"],
      compile=0,
      deps=js_deps + [
          ":" + name,
          "//javascript/closure/testing:testsuite",
      ],
      deps_mgmt="closure",
      externs_list=["//javascript/externs:common"],
      jvm_flags=["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
      data=["//testing/matrix/nativebrowsers/chrome:stable_data"],
  )

  native.jsunit_test(
      name="compiled_test",
      srcs=["TestHarness.js"],
      compile=1,
      compiler="//javascript/tools/jscompiler:head",
      defs=_CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--j2cl_pass",
          "--export_test_functions=true",
          "--language_in=ECMASCRIPT6_STRICT",
          "--language_out=ECMASCRIPT5",
          "--property_renaming=OFF",
          "--pretty_print",
          "--norewrite_polyfills",
          "--strict",
          "--variable_renaming=OFF",
          "--jscomp_off=lateProvide",
      ] + defs,
      deps=js_deps + [
          ":" + name,
          "//javascript/closure/testing:testsuite",
      ],
      deps_mgmt="closure",
      externs_list=["//javascript/externs:common"],
      jvm_flags=["-Dcom.google.testing.selenium.browser=CHROME_LINUX"],
      data=["//testing/matrix/nativebrowsers/chrome:stable_data"],
  )

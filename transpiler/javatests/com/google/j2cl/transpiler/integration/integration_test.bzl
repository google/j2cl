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

load("/third_party/java_src/j2cl/build_def/j2cl_util", "J2CL_UNOPTIMIZED_DEFS",
     "J2CL_OPTIMIZED_DEFS", "J2CL_TEST_DEFS")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_package")
load("/tools/build_defs/label/def", "absolute_label")
load("//testing/web/build_defs:web.bzl", "web_test")
load("//testing/web/build_defs/js:js.bzl", "jsunit_test")


def integration_test(name,
                     srcs,
                     deps=[],
                     defs=[],
                     native_srcs=[],
                     js_deps=[],
                     main_class=None,
                     enable_gwt=False,
                     gwt_deps=[],
                     closure_defines=dict(),
                     generate_build_test=None,
                     test_externs_list=None,
                     disable_uncompiled_test=False,
                     disable_compiled_test=False,
                     plugins = [],
                    ):
  """Macro that turns Java files into integration test targets.

  deps are Labels of j2cl_library() rules. NOT labels of
  java_library() rules.
  """
  # figure out the current location
  java_package = get_java_package(PACKAGE_NAME)

  if not main_class:
    main_class = java_package + ".Main"

  if not test_externs_list:
    test_externs_list = ["//javascript/externs:common"]

  deps = [absolute_label(dep) for dep in deps]

  optimized_extra_defs = [
      # OPTIMIZE ENUMS:
      # TODO(cromwellian): investigate why JSCompiler doesn't preserve original
      # name before ReplaceStrings sees it.
      "--replace_strings=module$exports$nativebootstrap$Util$impl.$makeEnumName(?)",
      # Polyfill re-write is disabled so that size tracking only focuses on
      # size issues that are actionable outside of JSCompiler or are expected
      # to eventually be addressed inside of JSCompiler.
      "--rewrite_polyfills=false",
      # Cuts optimize time nearly in half and the optimization leaks that it
      # previously hid no longer exist.
      "--closure_entry_point=gen.opt.Harness",
  ]

  # Since integration tests are used for optimized size tracking, set
  # behavior to the mode with the smallest output size which is what we expect
  # will also be used for customer application production releases. If some
  # particular test needs one of these on they can override with
  # closure_defines.
  defines = {
      # Turn on assertions since the integration tests rely on them.
      "ASSERTIONS_ENABLED_" : "true",
      "jre.checks.checkLevel" : "MINIMAL",
      "jre.checkedMode" : "DISABLED",
      "jre.logging.logLevel" : "OFF",
  }

  defines.update(closure_defines)

  define_flags = ["--define=%s=%s" % (k,v) for (k,v) in defines.items()]

  defs = defs + define_flags

  j2cl_library(
      name=name,
      srcs=srcs,
      generate_build_test=generate_build_test,
      deps=deps,
      javacopts=[
          "-source 8",
          "-target 8",
          "-Xep:EqualsIncompatibleType:OFF",
          "-Xep:SelfComparison:OFF",  # See go/self-comparison-lsc
          "-Xep:SelfEquals:OFF",  # See go/self-equals-lsc
          "-Xep:SelfEquality:OFF",
          "-Xep:LoopConditionChecker:OFF",
          "-Xep:IdentityBinaryExpression:OFF",
      ],
      _js_deps=js_deps,
      native_srcs=native_srcs,
      _test_externs_list=test_externs_list,
      plugins = plugins,
  )

  # blaze build :optimized_js
  opt_harness = """
      goog.module('gen.opt.Harness');
      var Main = goog.require('%s');
      Main.m_main__arrayOf_java_lang_String([]);
  """ % main_class
  _genfile("OptHarness.js", opt_harness)

  native.js_binary(
      name="optimized_js",
      srcs=["OptHarness.js"],
      defs=J2CL_OPTIMIZED_DEFS + optimized_extra_defs + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list= test_externs_list,
      deps=[":" + name],
  )
  # For constructing readable optimized diffs.
  readable_out_defs = [ "--variable_renaming=OFF" , "--property_renaming=OFF" , "--pretty_print"]
  native.js_binary(
      name="readable_optimized_js",
      srcs=["OptHarness.js"],
      defs= J2CL_OPTIMIZED_DEFS + readable_out_defs + optimized_extra_defs + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=test_externs_list,
      deps=[":" + name],
  )
  # For constructing readable unoptimized diffs.
  native.js_binary(
      name="readable_unoptimized_js",
      srcs=["OptHarness.js"],
      defs=J2CL_UNOPTIMIZED_DEFS + readable_out_defs + defs,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=test_externs_list,
      deps=[":" + name],
  )

  # For constructing GWT transpiled output.
  if enable_gwt:
    _gwt_targets(java_package, srcs, deps, gwt_deps)

  # blaze test :uncompiled_test
  # blaze test :compiled_test

  test_harness = """
      goog.module('gen.test.Harness');
      goog.setTestOnly();

      var testSuite = goog.require('goog.testing.testSuite');
      var Main = goog.require('%s');
      testSuite({
        test_Main: function() {
          Main.m_main__arrayOf_java_lang_String([]);
        }
      });
  """ % (main_class)
  _genfile("TestHarness.js", test_harness)

  jsunit_test_args = dict(
      srcs=["TestHarness.js"],
      deps=[
          ":" + name,
          "//javascript/closure/testing:testsuite",
      ],
      deps_mgmt="closure",
      defs=J2CL_TEST_DEFS + [
          "--closure_entry_point=gen.test.Harness"
      ] + defs,
      externs_list=test_externs_list,
      jvm_flags=[
          "-Djsrunner.net.useJsBundles=true"
      ],
  )

  jsunit_test(
      name="uncompiled_test_debug",
      tags=["manual", "notap"],
      **jsunit_test_args
  )

  web_test(
      name="uncompiled_test",
      browser="//testing/web/browsers:chrome-linux",
      tags=["manual", "notap"] if disable_uncompiled_test else [],
      test=":uncompiled_test_debug"
  )

  jsunit_test(
      name="compiled_test_debug",
      compile=1,
      compiler="//javascript/tools/jscompiler:head",
      tags=["manual", "notap"],
      **jsunit_test_args
  )

  web_test(
      name="compiled_test",
      browser="//testing/web/browsers:chrome-linux",
      tags=["manual", "notap"] if disable_compiled_test else [],
      test=":compiled_test_debug"
  )


def _gwt_targets(java_package, srcs, deps, gwt_deps):
  if any([".srcjar" in src for src in srcs]):
    fail("gwt_module cannot handle srcjar")

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
  _genfile("MainEntryPoint.java", gwt_harness)

  java_library_deps = gwt_deps if gwt_deps else [dep + "_java_library" for dep in deps]
  native.gwt_module(
      name="gwt_module",
      srcs=srcs + ["MainEntryPoint.java"],
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

  native.gwt_application(
      name="optimized_gwt_application",
      compiler_opts=[
          "-optimize 9",
          "-style OBFUSCATED",
          "-setProperty user.agent=safari",
          "-setProperty compiler.stackMode=strip",
          "-setProperty compiler.enum.obfuscate.names=true",
          "-setProperty jre.logging.logLevel=OFF",
          "-setProperty document.compatMode.severity=IGNORE",
          "-setProperty user.agent.runtimeWarning=false",
          "-setProperty jre.checks.checkLevel=MINIMAL",
          "-XnoclassMetadata",
      ],
      shard_count=1,
      module_target=":gwt_module",
      tags=["manual"],
  )

def _genfile(name, str):
  native.genrule(
      name=name.replace(".", "_"),
      outs=[name],
      cmd="echo \"%s\" > $@" % str,
  )

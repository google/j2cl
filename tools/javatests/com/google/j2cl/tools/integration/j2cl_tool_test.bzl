"""jsni_converter_test macro
This build extension defines a macro named jsni_converter_test that can be used to produce
end-to-end test for the jsni converter. This macro takes as input: java files, the
expected generated js files and the js tests to run. The macro converts the java files by using
jsni_to_j2cl_converter rule, unzips the generated javascript files, produces a js library and emits
a js tests.


Here is an example use of jsni_converter_test:

jsni_converter_test(
    name = "simple_test",
    java_files = [
      "Bar.java",
      "Foo.java",
    ],
    generated_js_files = [
      "com/google/j2cl/tools/jsni/src/Bar.native.js",
      "com/google/j2cl/tools/jsni/src/Bar$Inner.native.js",
      "com/google/j2cl/tools/jsni/src/Foo.native.js"
    ],
    js_test_files = [
      "simple_test.js"
    ]
)

"""

# TODO (dramaix) Simplify this rule once the closure compiler support zip files: we could have
# java files with native method as test input, run it through the shared rules with JRE transpile
# and run the tests that are actually written in java instead of javascript.

load("/third_party/java_src/j2cl/build_def/j2cl_tool", "jsni_to_j2cl_converter")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_root")

def jsni_converter_test(
    java_files=[], generated_js_files=[], js_test_files=[], **kwargs):

  rule_name = kwargs["name"]
  converter_rule_name = rule_name + "_converter"
  unzipper_rule_name = rule_name + "_unzipper"
  js_library_rule_name = rule_name + "_js_library"


  jsni_to_j2cl_converter(
    name = converter_rule_name,
    srcs = java_files,
  )

  java_root = get_java_root(PACKAGE_NAME)
  unzip_directory = "$(GENDIR)/" + java_root
  zip_file = "$(location :" + converter_rule_name +")"

  # TODO (dramaix) this rule is needed because blaze is not yet ready to process JS zip bundles.
  # Once it's the case, remove this rule.
  native.genrule(
    name = unzipper_rule_name,
    cmd = " ".join([
      "$(location //third_party/unzip:unzip) -o " + zip_file + " -d " + unzip_directory + ";",
      # find all files containing a $ in their name
      "for file in $$(find " + unzip_directory + " -name \"*\$$*.native.js\");",
      # and replace $ by _ (blaze doesn't support file with $)
      "do mv \"$$file\" \"$${file//$$/_}\";",
      "done;"]),
    srcs = [":" + converter_rule_name],
    outs = generated_js_files,
    tools=[
      "//third_party/unzip",
      ],
    testonly = 1,
  )

  native.js_library(
    name = js_library_rule_name,
    srcs = [
      "Mock.js",
      ":" + unzipper_rule_name,
    ],
    testonly = 1,
  )

  native.jsunit_test(
    name = rule_name,
    srcs = js_test_files,
    deps=[":" + js_library_rule_name],
    jvm_flags=[
      "-Dcom.google.testing.selenium.browser=CHROME_LINUX"
    ],
    data=["//testing/matrix/nativebrowsers/chrome:stable_data",],
  )



"""jsni_converter_test build macro

Defines an integration test (from input Java source and input expected generated
JS) that runs some output in a jsunit_test() rule.


Example usage:

jsni_converter_test()

"""


# TODO (dramaix) Simplify this rule once the closure compiler support zip
# files: we could have java files with native method as test input, run it
# through the shared rules with JRE transpile and run the tests that are
# actually written in java instead of javascript.


load("//build_def:jsni_to_native_js_bundle.bzl",
     "jsni_to_native_js_bundle")
load("//build_def:j2cl_library.bzl", "j2cl_library")
load("//build_def:j2cl_test.bzl", "j2cl_test")


def jsni_converter_test():

  srcs = native.glob(["*.java"], exclude=["Test.java"])

  jsni_to_native_js_bundle(
      name = "native_bundle",
      srcs = srcs,
      deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
  )

  j2cl_library(
      name = "test_library",
      srcs = srcs,
      native_srcs_zips = [":native_bundle"],
      testonly = 1,
      deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
  )

  j2cl_test(
      name = "Test",
      srcs = ["Test.java"],
      deps = [
          ":test_library",
          "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
          "//third_party/java/junit:junit-j2cl",
      ],
  )

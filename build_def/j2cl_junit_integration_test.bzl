load("/third_party/java_src/j2cl/build_def/j2cl_java_test", "j2cl_java_test")
load("/third_party/java_src/j2cl/build_def/j2cl_java_test", "J2CL_JAVA_TEST_CLOSURE_DEFS")


def j2cl_junit_integration_test(name, testCase, deps = []):
  j2cl_java_test(
      name = name,
      srcs = [testCase],
      # This test is supposed to fail and is used to test that failing tests indeed trigger failure in
      # our cross compiled JUnit tests, so we are making it manual for forge to ignore it.
      tags = ["manual", "notap",],
      deps = ["//third_party/java/junit"] + deps,
  )

  native.js_library(
      name = name + "_extract_js_lib",
      testonly = 1,
      srcs = [":" + name + "_extract_js"],
  )

  native.js_binary(
      name = name + "_binary",
      testonly = 1,
      defs = J2CL_JAVA_TEST_CLOSURE_DEFS,
      deps = [
          ":" + name + "_js_library",
          ":" + name + "_extract_js_lib",
          "//javascript/closure/testing:testsuite",
      ],
  )


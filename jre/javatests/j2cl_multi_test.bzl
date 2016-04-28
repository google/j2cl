"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("/third_party/java/j2cl/j2cl_test", "j2cl_test")

def j2cl_multi_test(name, test_class):
  deps = [":emul_tests_lib", "//third_party/java/junit"]
  srcs = [test_class.replace(".", "/") + ".java"]
  j2cl_test(name = name, test_class = test_class, compile = 1, srcs = srcs, deps = deps)
  j2cl_test(name = name + "_debug", test_class = test_class, compile = 0, srcs = srcs, deps = deps)

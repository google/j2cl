"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    deps = [":some_dep"],
)

"""

load("/third_party/java/j2cl/j2cl_test", "j2cl_test")

def j2cl_multi_test(name, test_class, **kwargs):
  j2cl_test(name = name, test_class = test_class, compile = 1, **kwargs)
  j2cl_test(name = name + "_debug", test_class = test_class, compile = 0, **kwargs)
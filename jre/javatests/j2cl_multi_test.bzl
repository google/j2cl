"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("//third_party/java/j2cl:j2cl_test.bzl", "j2cl_test")


def j2cl_multi_test(name, test_class, **kwargs):
  deps = [":emul_tests_lib", "//third_party/java/junit:junit-j2cl"]
  j2cl_test(name=name,
            test_class=test_class,
            generate_build_test=False,
            runtime_deps=deps,
            **kwargs)
  j2cl_test(name=name + "_compiled",
            test_class=test_class,
            compile=1,
            generate_build_test=False,
            externs_list=["//javascript/externs:common", "//javascript/externs:svg"],
            runtime_deps=deps,
            **kwargs)

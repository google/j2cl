"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("/third_party/java/j2cl/j2cl_test", "j2cl_test")


def j2cl_multi_test(name, test_class, **kwargs):
  deps = [":emul_tests_lib", "//third_party/java/junit:junit-j2cl"]
  srcs = [test_class.replace(".", "/") + ".java"]
  j2cl_test(name=name,
            test_class=test_class,
            generate_build_test=False,
            srcs=srcs,
            deps=deps,
            **kwargs)
  j2cl_test(name=name + "_compiled",
            test_class=test_class,
            compile=1,
            generate_build_test=False,
            srcs=srcs,
            deps=deps,
            extra_defs= [
                # JRE tests use $doc and do other illegal things on purpose.
                # b/33055288
                "--jscomp_off=undefinedVars",
                "--jscomp_off=checkTypes",
            ],
            **kwargs)

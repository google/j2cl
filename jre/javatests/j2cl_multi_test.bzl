"""j2cl_multi_test build rule

Creates a j2cl_test target for compiled and uncompiled mode.


Example use:

j2cl_multi_test(
    name = "my_test",
    test_class = ["MyJavaTestFile.java"],
)

"""

load("/third_party/java/j2cl/j2cl_test", "j2cl_test")
load("/third_party/java_src/j2cl/build_def/j2cl_util", "J2CL_OPTIMIZED_DEFS")


def j2cl_multi_test(name, test_class, **kwargs):
  deps = [":emul_tests_lib", "//third_party/java/junit"]
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
            defs=J2CL_OPTIMIZED_DEFS + [
                # JRE tests use $doc and do other illegal things on purpose.
                "--jscomp_off=undefinedVars",
                "--jscomp_off=checkTypes",
            ],
            **kwargs)

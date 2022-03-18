"""j2cl_test build macro
Works similarly to junit_test; see j2cl_test_common.bzl for details
"""

load(":j2cl_test_common.bzl", "j2cl_test_common")

# buildifier: disable=function-docstring-args
def j2cl_test(
        name,
        tags = [],
        **kwargs):
    """Macro for running a JUnit test cross compiled as a web test

       This macro uses the j2cl_test_tranpile macro to transpile tests and feed
       them into a jsunit_test.
    """

    j2cl_test_common(
        name,
        tags = tags + ["j2cl"],
        **kwargs
    )

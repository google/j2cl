"""j2kt_test build macro
Works similarly to junit_test; see j2cl_test_common.bzl for details
"""

load(":j2cl_test_common.bzl", "j2cl_test_common")

def j2kt_jvm_test(name, tags = [], **kwargs):
    j2cl_test_common(
        name,
        platform = "J2KT-JVM",
        tags = tags + ["j2kt"],
        **kwargs
    )

def j2kt_native_test(name, tags = [], **kwargs):
    j2cl_test_common(
        name,
        platform = "J2KT-NATIVE",
        tags = tags + ["j2kt"],
        **kwargs
    )

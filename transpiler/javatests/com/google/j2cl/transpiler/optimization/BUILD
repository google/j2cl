# Description:
#   j2cl tests verifying important optimizations

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("j2cl_optimization_test", "j2cl_optimization_test")

j2cl_optimization_test(
    name = "BooleansTest",
)

j2cl_optimization_test(
    name = "StringsTest",
)

j2cl_optimization_test(
    name = "ArraysTest",
    defs = [
        "--define=ARRAY_CHECK_BOUNDS_=false",
        "--define=ARRAY_CHECK_TYPES_=false",
    ],
)

j2cl_optimization_test(
    name = "ArrayListsTest",
    defs = [
        "--define=jre.checks.checkLevel=MINIMAL",
        "--define=jre.checkedMode=DISABLED",
        "--define=ARRAY_CHECK_BOUNDS_=false",
        "--define=ARRAY_CHECK_TYPES_=false",
    ],
)

j2cl_optimization_test(
    name = "CastDisableOptimizationTest",
    defs = ["--define=CAST_CHECKS_ENABLED_=false"],
)

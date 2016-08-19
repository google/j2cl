# Description:
#   j2cl tests verifying important optimizations

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load("j2cl_optimization_test", "j2cl_optimization_test")

j2cl_optimization_test(
    name = "BooleansTest",
    javacopts = ["-Xep:SelfEquality:OFF"],
)

j2cl_optimization_test(
    name = "StringsTest",
)

j2cl_optimization_test(
    name = "ArraysTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

j2cl_optimization_test(
    name = "ArrayListsTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

j2cl_optimization_test(
    name = "CastDisableOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

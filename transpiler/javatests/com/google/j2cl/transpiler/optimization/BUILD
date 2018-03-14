# Description:
#   j2cl tests verifying important optimizations

# Apache2
licenses(["notice"])

load(":j2cl_optimization_test.bzl", "j2cl_optimization_test")

j2cl_optimization_test(
    name = "BooleansTest",
    javacopts = [
        "-XepDisableAllChecks",
    ],
)

j2cl_optimization_test(
    name = "StringsTest",
    javacopts = [
        "-XepDisableAllChecks",
    ],
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

j2cl_optimization_test(
    name = "EnumOptimizationTest",
    defs = ["--define=jre.checks.checkLevel=MINIMAL"],
)

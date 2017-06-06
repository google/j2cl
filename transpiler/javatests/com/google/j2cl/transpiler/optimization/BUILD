# Description:
#   j2cl tests verifying important optimizations

package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load(":j2cl_optimization_test.bzl", "j2cl_optimization_test")

j2cl_optimization_test(
    name = "BooleansTest",
    javacopts = [
        "-Xep:SelfEquality:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
    ],
)

j2cl_optimization_test(
    name = "StringsTest",
    javacopts = [
        "-Xep:SelfEquality:OFF",
        "-Xep:IdentityBinaryExpression:OFF",
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

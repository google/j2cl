load("//build_defs:rules.bzl", "j2cl_library")
load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/readable:__subpackages__"],
    licenses = ["reciprocal"],
)

java_library(
    name = "inheritedstaticmembersdep",
    srcs = glob(["*.java"]),
)

# J2KT-JVM uses the untranspiled Java library, like it does for Java protos, so that the
# package-private declaring type is not accessible from Kotlin in other packages.
alias(
    name = "inheritedstaticmembersdep-j2kt-jvm",
    actual = "inheritedstaticmembersdep",
)

j2cl_library(
    name = "inheritedstaticmembersdep-j2cl",
    srcs = glob(["*.java"]),
)

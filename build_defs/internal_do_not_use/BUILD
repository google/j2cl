# Description:
#   Build rules used by J2CL and BUILD targets used by the rules.

load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")

licenses(["notice"])  # Apache 2.0

##########################################################
#                                                        #
#  __          __     _____  _   _ _____ _   _  _____    #
#  \ \        / /\   |  __ \| \ | |_   _| \ | |/ ____|   #
#   \ \  /\  / /  \  | |__) |  \| | | | |  \| | |  __    #
#    \ \/  \/ / /\ \ |  _  /| . ` | | | | . ` | | |_ |   #
#     \  /\  / ____ \| | \ \| |\  |_| |_| |\  | |__| |   #
#      \/  \/_/    \_\_|  \_\_| \_|_____|_| \_|\_____|   #
#                                                        #
#                                                        #
##########################################################
# Never depend on any of the targets in this BUILD file  #
# manually. They are used within tools/build rules and   #
# and should actually be private, but Blaze does not     #
# support this yet, b/34359566.                          #
##########################################################

package(default_visibility = ["//visibility:public"])

# ***************************************************
# Transpiler related
# ***************************************************

# Main J2CL transpiler binary.
alias(
    name = "BazelJ2clBuilder",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2clBuilder",
)

# TODO(goktug): remove workaround after b/71772385 is fixed
genrule(
    name = "dummy_src",
    outs = ["package-info.java"],
    cmd = "echo \"package dummy_;\" > $@",
)

# ***************************************************
# Standard library related
# ***************************************************

alias(
    name = "jre",
    actual = "//jre/java:jre",
)

default_java_toolchain(
    name = "j2cl_java_toolchain",
    bootclasspath = ["//jre/java:libjre_bootclasspath.jar"],
)

# ***************************************************
# JUnit library related
# ***************************************************

alias(
    name = "internal_junit_annotations",
    actual = "//junit/generator/java/com/google/j2cl/junit/apt:internal_junit_annotations",
)

# ***************************************************
# Tools related
# **************************************************

alias(
    name = "GwtIncompatibleStripper",
    actual = "//tools/java/com/google/j2cl/tools/gwtincompatible:GwtIncompatibleStripper_worker",
)

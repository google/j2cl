# Description:
#   Build rules used by J2CL and BUILD targets used by the rules.

package(
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],  # Apache 2.0
)

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

# ***************************************************
# Transpiler related
# ***************************************************

# Main J2CL transpiler binary.
alias(
    name = "BazelJ2clBuilder",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2clBuilder",
)

# ***************************************************
# Standard library related
# ***************************************************

alias(
    name = "j2cl_java_toolchain",
    actual = "//jre/java:j2cl_java_toolchain",
)

alias(
    name = "j2wasm_java_toolchain",
    actual = "//jre/java:j2wasm_java_toolchain",
)

alias(
    name = "j2wasm_jre",
    actual = "//jre/java:jre-j2wasm",
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

alias(
    name = "J2clRta",
    actual = "//tools/java/com/google/j2cl/tools/rta:J2clRta_worker",
)

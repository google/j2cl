# Description:
#   Build rules in this directory are used by our tooling and build rules

# Apache2
licenses(["notice"])

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
    name = "J2clTranspiler",
    actual = "//transpiler/java/com/google/j2cl/transpiler:J2clTranspiler_worker",
)

# ***************************************************
# Standard library related
# ***************************************************

# ***************************************************
# JUnit library related
# ***************************************************

# ***************************************************
# Tools related
# **************************************************

alias(
    name = "GwtIncompatibleStripper",
    actual = "//tools/java/com/google/j2cl/tools/gwtincompatible:GwtIncompatibleStripper_worker",
)

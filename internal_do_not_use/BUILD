# Description:
#   Build rules in this directory are used by our tooling and build rules

# Google owns the copyright
licenses(["unencumbered"])

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

# Performs JUnit specific translation.
java_plugin(
    name = "junit_processor",
    processor_class = "com.google.j2cl.junit.apt.J2clTestingProcessor",
    deps = ["//junit/java/com/google/j2cl/junit/apt:junit_processor"],
)

# ***************************************************
# Tools related
# **************************************************

java_binary(
    name = "JsniConverter",
    main_class = "com.google.j2cl.tools.jsni.Runner",
    visibility = [
        "//:__subpackages__",
        "//java/com/google/protobuf/contrib/gwt:__pkg__",
        "//third_party/java_src/jbox2d:__subpackages__",
    ],
    runtime_deps = ["//tools/java/com/google/j2cl/tools/jsni"],
)

java_binary(
    name = "GwtIncompatibleStripper",
    main_class = "com.google.j2cl.tools.gwtincompatible.Stripper",
    visibility = ["//visibility:public"],
    runtime_deps = ["//tools/java/com/google/j2cl/tools/gwtincompatible"],
)

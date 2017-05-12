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

load("//third_party/java/j2cl:j2cl_library.bzl", "j2cl_library")

# ***************************************************
# Transpiler related
# ***************************************************

# Main J2CL transpiler binary.
java_binary(
    name = "J2clTranspiler",
    main_class = "com.google.j2cl.transpiler.J2clTranspilerWorker",
    runtime_deps = ["//transpiler/java/com/google/j2cl/transpiler:transpiler_worker_lib"],
)

# ***************************************************
# Standard library related
# ***************************************************

j2cl_library(
    name = "jre",
    exports = ["//jre/java:jre"],
)

# ***************************************************
# JUnit library related
# ***************************************************

# Performs JUnit specific translation.
java_plugin(
    name = "junit_processor",
    processor_class = "com.google.j2cl.junit.apt.J2clTestingProcessor",
    deps = ["//junit/java/com/google/j2cl/junit/apt:junit_processor"],
)

j2cl_library(
    name = "internal_junit_annotations",
    exports = ["//junit/java/com/google/j2cl/junit/apt:internal_junit_annotations"],
)

# ***************************************************
# Tools related
# **************************************************

java_binary(
    name = "JsniConverter",
    main_class = "com.google.j2cl.tools.jsni.JsniConverter",
    visibility = [
        "//java/com/google/protobuf/contrib/gwt:__pkg__",
        "//:__subpackages__",
        "//third_party/java_src/jbox2d:__subpackages__",
    ],
    runtime_deps = ["//tools/java/com/google/j2cl/tools/jsni"],
)

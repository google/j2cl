# Description:
#   Build rules used by J2CL and BUILD targets used by the rules.

load(":j2cl_library.bzl", "j2cl_library")
load(":j2kt_library.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(":j2wasm_library.bzl", "j2wasm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
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

config_setting(
    name = "j2kt_web_enabled",
    flag_values = {
        "//:experimental_enable_j2kt_web": "True",
    },
)

# ***************************************************
# Standard library related
# ***************************************************

alias(
    name = "j2wasm_jre",
    actual = "//jre/java:jre-j2wasm",
)

# ***************************************************
# JUnit related
# ***************************************************

j2cl_library(
    name = "internal_junit_annotations",
    testonly = 1,
    exports = [
        "//junit/generator/java/com/google/j2cl/junit/apt:internal_junit_annotations",
    ],
)

j2kt_native_library(
    name = "internal_junit_runtime-j2kt-native",
    testonly = 1,
    exports = [
        "//junit/generator/java/com/google/j2cl/junit/runtime:testcase_violator-j2kt-native",
        "//third_party:junit-j2kt-native",
    ],
)

j2kt_jvm_library(
    name = "internal_junit_runtime-j2kt-jvm",
    testonly = 1,
    exports = [
        "//junit/generator/java/com/google/j2cl/junit/runtime:testcase_violator-j2kt-jvm",
        "//third_party:junit-j2kt-jvm",
    ],
)

j2wasm_library(
    name = "internal_junit_runtime-j2wasm",
    testonly = 1,
    exports = [
        "//junit/generator/java/com/google/j2cl/junit/runtime:internal_assumption_violated_exception-j2wasm",
        "//junit/generator/java/com/google/j2cl/junit/runtime:jsunit_helpers-j2wasm",
        "//junit/generator/java/com/google/j2cl/junit/runtime:testcase_violator-j2wasm",
    ],
)

j2cl_library(
    name = "internal_junit_runtime",
    testonly = 1,
    exports = [
        "//:jsinterop-annotations-j2cl",
        "//junit/generator/java/com/google/j2cl/junit/runtime:internal_assumption_violated_exception-j2cl",
        "//junit/generator/java/com/google/j2cl/junit/runtime:jsunit_helpers-j2cl",
        "//junit/generator/java/com/google/j2cl/junit/runtime:testcase_violator-j2cl",
    ],
)

alias(
    name = "internal_j2cl_test_suite",
    actual = "//junit/generator/java/com/google/j2cl/junit/runtime:internal_j2cl_test_suite",
)

alias(
    name = "closure_testcase",
    actual = "//third_party:closure_testcase",
)

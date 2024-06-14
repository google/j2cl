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

# Main J2CL transpiler binary.
alias(
    name = "BazelJ2clBuilder",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2clBuilder",
)

alias(
    name = "BazelJ2clBuilderWithKotlinSupport",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2clBuilderWithKotlinSupport",
    visibility = ["//build_defs/internal_do_not_use:kotlin_allowlist"],
)

# WARNING: Don't increase the visibility without talking to Kotlin team.
package_group(
    name = "kotlin_allowlist",
    packages = [
    ],
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
    name = "internal_parametrized_test_suite",
    actual = "//junit/generator/java/com/google/j2cl/junit/runtime:internal_parametrized_test_suite",
)

alias(
    name = "closure_testcase",
    actual = "//third_party:closure_testcase",
)

alias(
    name = "closure_testsuite",
    actual = "//third_party:closure_testsuite",
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

alias(
    name = "J2wasmExportGenerator",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2wasmExportGenerator",
)

alias(
    name = "J2wasmBundler",
    actual = "//transpiler/java/com/google/j2cl/transpiler:BazelJ2wasmBundler",
)

alias(
    name = "binaryen",
    actual = "//third_party:binaryen",
)

alias(
    name = "binaryen-legacy",
    actual = "//third_party:binaryen-legacy",
)

# bzl_librarys

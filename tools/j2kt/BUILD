# Description:
#  Python scripts that create the helper interop headerts for J2KT to make the migration from
#  J2ObjC easier.

load("//devtools/python/blaze:pytype.bzl", "pytype_strict_binary", "pytype_strict_contrib_test", "pytype_strict_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

pytype_strict_library(
    name = "generate_j2kt_interop_header_lib",
    srcs = ["generate_j2kt_interop_header.py"],
    visibility = [
        "//build_defs/internal_do_not_use:__subpackages__",
    ],
    deps = ["//third_party/py/absl:app"],
)

pytype_strict_binary(
    name = "generate_j2kt_interop_header",
    srcs = ["generate_j2kt_interop_header.py"],
    main = "generate_j2kt_interop_header.py",
    visibility = [
        "//build_defs/internal_do_not_use:__subpackages__",
    ],
    deps = [":generate_j2kt_interop_header_lib"],
)

pytype_strict_contrib_test(
    name = "generate_j2kt_interop_header_test",
    srcs = [
        "generate_j2kt_interop_header_test.py",
    ],
    deps = [
        ":generate_j2kt_interop_header_lib",
        "//testing/pybase",
        "//testing/pybase:parameterized",
    ],
)

# Description:
#   j2cl/vmbootstrap: The third layer of JS utility functions.
#
# Depends on the JRE JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

js_library(
    name = "vmbootstrap",
    deps = [
        ":Arrays",
        ":Asserts",
    ],
)

js_library(
    name = "Asserts",
    srcs = ["Asserts.js"],
)

js_library(
    name = "Arrays",
    srcs = ["Arrays.js"],
)

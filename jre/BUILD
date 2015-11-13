# Description:
#   j2cl/jre: JRE subset for transpilation.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

# Re-expose the JRE library at the top level.
js_library(
    name = "jre",
    deps = ["//jre/java:jre"],
)

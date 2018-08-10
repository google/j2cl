"""j2cl_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2cl_import() target to be directly depended upon from j2cl_library() targets.

Should only be used for importing annotation byte code, otherwise may result
in hard to debug errors!


Example use:

j2cl_import(
  name = "Qux",
  jar = "//java/com/qux:qux_java",
)

j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
    deps = [":Qux"],  # the j2cl_import target
)

"""

load("//build_def:j2cl_java_library.bzl", "j2cl_java_library")

def j2cl_import(
        name,
        jar,
        licenses = None,
        visibility = None):
    """Translates nonstandard inputs into a dep'able j2cl_library.

    Args:
      jar: Jar file to appropriately rename.
    """

    # exit early to avoid parse errors when running under bazel
    if not hasattr(native, "js_library"):
        return

    j2cl_java_library(
        name = name + "_java_library",
        restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
        exports = [jar],
        licenses = licenses,
        visibility = visibility,
        # Direct automated dep picking tools away from this target.
        tags = ["avoid_dep"],
    )

    # Empty target to satisfy references from j2cl_library()s
    native.js_library(
        name = name,
        licenses = licenses,
        visibility = visibility,
        # Direct automated dep picking tools away from this target.
        tags = ["avoid_dep"],
    )

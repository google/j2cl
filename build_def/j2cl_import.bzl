"""j2cl_import build macro

Takes nonstandard input and repackages it with names that will allow the
j2cl_import() target to be directly depended upon from j2cl_library() targets.
The nonstandard input can be a source jar, a jar, or both a jar and js.


Example use:

# creates properly named forwarding rules
#     java_library(name="Qux_java_library")
#     js_library(name="Qux")
j2cl_import(
  name = "Qux",
  jar = "//java/com/qux:qux_java",  # nonconforming name
  js = "//java/com/qux:qux_js-lib",  # nonconforming name
)

# creates js_library(name="Bar") containing the results.
j2cl_library(
    name = "Bar",
    srcs = glob(["Bar.java"]),
    deps = [":Qux"],  # the j2cl_import target
)

"""


load("/third_party/java/j2cl/j2cl_library", "j2cl_library")


def j2cl_import(name,
                jar,
                js=None,
                licenses=None,
                visibility=None):
  """Translates nonstandard inputs into a dep'able j2cl_library.

  Args:
    jar: Jar file to appropriately rename.
    js: JS file to appropriately rename.
  """

  # exit early to avoid parse errors when running under bazel
  if not hasattr(native, "js_library"):
    return

  native.java_library(
      name=name + "_java_library",
      restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
      exports=[jar],
      licenses=licenses,
      visibility=visibility,
      # Direct automated dep picking tools away from this target.
      tags=["avoid_dep"],
  )

  if js:
    native.js_library(
        name=name,
        exports=[js],
        deps_mgmt = "legacy",
        licenses=licenses,
        visibility=visibility,
        # Direct automated dep picking tools away from this target.
        tags=["avoid_dep"],
    )
  else:
    # Empty target to satisfy references from j2cl_library()s
    native.js_library(
        name=name,
        licenses=licenses,
        visibility=visibility,
        # Direct automated dep picking tools away from this target.
        tags=["avoid_dep"],
        deps_mgmt = "legacy",
    )

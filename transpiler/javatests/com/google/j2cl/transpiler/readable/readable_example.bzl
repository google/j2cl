"""readable_example build macro

Confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
readable_example(
    name = "foobar",
    srcs = glob(["*.java"]),
)

"""


load("/third_party/java_src/j2cl/build_def/j2cl_util", "J2CL_OPTIMIZED_DEFS")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")


def readable_example(
    name, srcs, native_srcs=[], native_srcs_pkg=None, deps=[], js_deps=[],
    plugins=[], _declare_legacy_namespace=False):
  """Macro that confirms the JS compilability of some transpiled Java.

  deps are Labels of j2cl_library() rules. NOT labels of
  java_library() rules.
  """

  # Transpile the Java files.
  j2cl_library(
      name=name,
      srcs=srcs,
      generate_build_test=False,
      javacopts=[
          "-source 8",
          "-target 8"
      ],
      native_srcs=native_srcs,
      native_srcs_pkg=native_srcs_pkg,
      deps=deps,
      plugins=plugins,
      _js_deps=js_deps,
      _readable_source_maps=True,
      _declare_legacy_namespace=_declare_legacy_namespace,
  )

  # Verify compilability of generated JS.
  native.js_binary(
      name=name + "_binary",
      defs=J2CL_OPTIMIZED_DEFS,
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=[":" + name],
  )

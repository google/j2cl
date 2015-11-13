"""readable_example build macro

Confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
readable_example(
    name = "foobar",
    srcs = glob(["*.java"]),
)

"""


load("/javascript/closure/builddefs", "CLOSURE_COMPILER_FLAGS_FULL_TYPED")
load("/third_party/java/j2cl/j2cl_library", "j2cl_library")


def readable_example(
    name, srcs, native_srcs=[], native_srcs_pkg=None, deps=[], js_deps=[]):
  """Macro that confirms the JS compilability of some transpiled Java.

  deps are Labels of j2cl_library() rules. NOT labels of
  java_library() rules.
  """

  # Transpile the Java files.
  j2cl_library(
      name=name,
      srcs=srcs,
      javacopts=[
          "-source 8",
          "-target 8"
      ],
      native_srcs=native_srcs,
      native_srcs_pkg=native_srcs_pkg,
      deps=deps,
  )

  # Verify compilability of generated JS.
  native.js_binary(
      name=name + "_binary",
      defs=CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--language_in=ECMASCRIPT6_STRICT",
          "--language_out=ECMASCRIPT5",
          "--remove_dead_code",
          "--remove_unused_vars",
          "--remove_unused_local_vars=ON",
          "--remove_dead_assignments",
          "--jscomp_off=lateProvide",
          "--jscomp_off=extraRequire",
          "--jscomp_off=suspiciousCode",
          "--jscomp_off=transitionalSuspiciousCodeWarnings",
          "--jscomp_off=uselessCode",
      ],
      compiler="//javascript/tools/jscompiler:head",
      externs_list=["//javascript/externs:common"],
      deps=js_deps + [":" + name],
  )

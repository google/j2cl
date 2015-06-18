"""readable_example build macro.

A build macro that confirms the JS compilability of some transpiled Java.


Example usage:

# Creates verification target
# blaze build :foobar_binary
readable_example(
    name = "foobar",
    srcs = glob(["*.java"]),
)
"""

load(
    "/third_party/java_src/j2cl/build_def/j2cl_java_library",
    "j2cl_java_library",
)

# TODO: source these flags from the authoritative location once they're
#       accessible from .bzl
CLOSURE_COMPILER_FLAGS_FULL_TYPED = [
    "--aggressive_var_check_level=WARNING",
    "--check_global_names_level=ERROR",
    "--check_provides=WARNING",
    "--closure_pass",
    "--collapse_properties",
    "--compute_function_side_effects=true",
    "--devirtualize_prototype_methods",
    "--inline_variables",
    "--jscomp_warning=deprecated",
    "--jscomp_warning=missingProperties",
    "--jscomp_warning=visibility",
    "--property_renaming=ALL_UNQUOTED",
    "--remove_unused_prototype_props",
    "--remove_unused_prototype_props_in_externs",
    "--smart_name_removal",
    "--variable_renaming=ALL",
    "--ambiguate_properties",
    "--disambiguate_properties",
]

def readable_example(name, srcs):
  """Macro that confirms the JS compilability of some transpiled Java."""
  # Transpile the Java files.
  j2cl_java_library(
      name = name,
      srcs = srcs,
  )

  # Verify compilability of generated JS.
  native.js_binary(
      name = name + "_binary",
      defs = CLOSURE_COMPILER_FLAGS_FULL_TYPED + [
          "--language_in=ECMASCRIPT6",
          "--language_out=ECMASCRIPT5",
          "--remove_dead_code",
          "--remove_unused_vars",
          "--remove_unused_local_vars=ON",
          "--remove_dead_assignments",
      ],
      compiler = "//javascript/tools/jscompiler:head",
      externs_list = ["//javascript/externs:common"],
      deps = [":" + name + "_js_library"],
  )

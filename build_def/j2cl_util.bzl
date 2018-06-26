"""Utility functions for the j2cl_* build rules / macros"""

def get_java_package(path):
  """Extract the java package from path"""

  segments = path.split("/")

  # Find different root start indecies based on potential java roots
  java_root_start_indecies = [_find(segments, root) for root in ["java", "javatests"]]

  # Choose the root that starts earliest
  start_index = min(java_root_start_indecies)

  if start_index == len(segments):
    fail("Cannot find java root: " + path)

  return ".".join(segments[start_index + 1:])

def _find(segments, s):
  return segments.index(s) if s in segments else len(segments)

load(
    "//javascript/tools/jscompiler/builddefs:flags.bzl",
    "ADVANCED_OPTIMIZATIONS_FLAGS",
    "USE_TYPES_FOR_OPTIMIZATIONS_FLAGS",
    "JS_TEST_FLAGS",
)

J2CL_UNOPTIMIZED_DEFS = [
    "--jscomp_error=strictMissingRequire",
    "--language_out=ECMASCRIPT5_STRICT",
]

# TODO(goktug): Switch to RECOMMENDED_FLAGS and opt-out from checks as needed.
J2CL_OPTIMIZED_DEFS = (J2CL_UNOPTIMIZED_DEFS +
                       ADVANCED_OPTIMIZATIONS_FLAGS +
                       USE_TYPES_FOR_OPTIMIZATIONS_FLAGS + [
                           "--extra_smart_name_removal=true",
                           "--define=goog.DEBUG=false",
                           "--remove_unused_prototype_props_in_externs",
                       ])

J2CL_TEST_DEFS = JS_TEST_FLAGS + [
    # Manage closure deps will strip our outputs in some tests
    "--manage_closure_dependencies=false",
    "--extra_smart_name_removal=true",
    # Enable assert statements for tests (as java_test does the same)
    "--remove_j2cl_asserts=false",
    # turn off the closure debug loader since it is causing warnings
    # in bundled mode
    "--define=goog.ENABLE_DEBUG_LOADER=false",
    # enables source maps for tests used in stack trace deobfuscation
    "--apply_input_source_maps",
    # set the naming pattern for the source map output
    "--create_source_map=%outname%.sourcemap",
]

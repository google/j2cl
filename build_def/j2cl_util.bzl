"""Utility functions for the j2cl_* build rules / macros"""

def _get_last_dir_occurrence_of(path, name):
  """Returns the index of the last occurrence of directory name in path."""
  if path == name:
    return 0
  if path.endswith("/" + name):
    return path.rfind(name)
  index = path.rfind("/" + name + "/")
  if index != -1:
    return index + 1
  if path.startswith(name + "/"):
    return 0
  else:
    return -1


def _get_java_root_index(pkg_name):
  """Returns the index of the java_root within a build package"""
  # Find the java folder in the beginning, middle or end of a path.
  java_index = _get_last_dir_occurrence_of(pkg_name, "java")

  # Find the javatests folder in the beginning, middle or end of a path.
  javatests_index = _get_last_dir_occurrence_of(pkg_name, "javatests")

  if java_index == -1 and javatests_index == -1:
    fail("can not find java root: " + pkg_name)

  if java_index > javatests_index:
    index = java_index + len("java/")
  else:
    index = javatests_index + len("javatests/")
  return index

def get_java_root(pkg_name):
  """Extract the path to java root from the build package"""
  return pkg_name[:_get_java_root_index(pkg_name)]

def get_java_path(pkg_name):
  """Extract the java path from the build package"""
  return pkg_name[len(get_java_root(pkg_name)):]

def get_java_package(pkg_name):
  """Extract the java package from the build package"""
  return get_java_path(pkg_name).replace("/", ".")

def generate_zip(name, srcs, pkg, testonly = None):
  """Generates a zip target with given srcs.

  See j2cl_library for details of pkg handling
  """
  # Exit early to avoid parse errors when running under bazel
  if not hasattr(native, "genzip"):
    return

  native.genzip(name=name, deps=[name + "_pkg_library"], testonly = testonly)

  if pkg == "RELATIVE":
    flatten = 0
    package_dir = None
    strip_prefix = None
  elif pkg == "CONVENTION":
    flatten = 1
    package_dir = get_java_path(native.package_name())
    strip_prefix = None
  else:
    fail("Incorrect package type: " + pkg)

  native.pkg_library(
      name=name + "_pkg_library",
      srcs=srcs,
      flatten=flatten,
      package_dir=package_dir,
      strip_prefix=strip_prefix,
      testonly = testonly,
  )

load(
    "//javascript/tools/jscompiler/builddefs:flags.bzl",
    "ADVANCED_OPTIMIZATIONS_FLAGS",
    "USE_TYPES_FOR_OPTIMIZATIONS_FLAGS",
    "JS_TEST_FLAGS",
)

J2CL_UNOPTIMIZED_DEFS = [
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

"""Utility functions for the j2cl_* build rules / macros"""

def _get_java_root_index(pkg_name):
  """Returns the index of the java_root within a build package"""
  # Find the java folder in the beginning, middle or end of a path.
  if pkg_name.endswith("java"):
    java_index = pkg_name.rfind("java")
  else:
    java_index = pkg_name.rfind("java/")

  # Find the javatests folder in the beginning, middle or end of a path.
  if pkg_name.endswith("javatests"):
    javatests_index = pkg_name.rfind("javatests")
  else:
    javatests_index = pkg_name.rfind("javatests/")

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
  elif pkg == "ABSOLUTE":
    flatten = 0
    package_dir = None
    strip_prefix = ""
  elif pkg == "CONVENTION":
    flatten = 1
    package_dir = get_java_path(PACKAGE_NAME)
    strip_prefix = None
  else:
    flatten = 1
    package_dir = pkg
    strip_prefix = None

  native.pkg_library(
      name=name + "_pkg_library",
      srcs=srcs,
      flatten=flatten,
      package_dir=package_dir,
      strip_prefix=strip_prefix,
      testonly = testonly,
  )

# Can't disable property renaming with a simple override because of
# Blaze bug b/28770521.
def make_output_readable(flags):
  new_flags = [flag for flag in flags if flag != "--variable_renaming=ALL"]
  return new_flags + [
      "--property_renaming=OFF",
      "--pretty_print",
  ]

load(
    "//javascript/tools/jscompiler/builddefs:flags.bzl",
    "ADVANCED_OPTIMIZATIONS_FLAGS",
    "USE_TYPES_FOR_OPTIMIZATIONS_FLAGS",
    "JS_TEST_FLAGS",
)

J2CL_UNOPTIMIZED_DEFS = [
    "--j2cl_pass=true",
    "--language_in=ECMASCRIPT6_STRICT",
    "--language_out=ECMASCRIPT5",
]

# TODO(goktug): Switch to RECOMMENDED_FLAGS and opt-out from checks as needed.
J2CL_OPTIMIZED_DEFS = (J2CL_UNOPTIMIZED_DEFS +
                       ADVANCED_OPTIMIZATIONS_FLAGS +
                       USE_TYPES_FOR_OPTIMIZATIONS_FLAGS +
                       ["--extra_smart_name_removal=true"])

J2CL_TEST_DEFS = JS_TEST_FLAGS + [
    # Manage closure deps will strip our outputs in some tests
    "--manage_closure_dependencies=false",
    "--j2cl_pass=true",
]

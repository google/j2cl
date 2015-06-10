"""Utility functions for the j2cl_* build rules / macros"""

def _get_java_root_index(pkg_name):
  """Returns the index of the java_root within a build package"""
  java_index = pkg_name.rfind("java/")
  javatests_index = pkg_name.rfind("javatests/")

  if java_index == -1 and javatests_index == -1:
    fail("can not find java root")

  if java_index > javatests_index:
    index = java_index + len("java/")
  else:
    index = javatests_index + len("javatests/")
  return index

def get_java_root(pkg_name):
  """Extract the path to java root from the build package"""
  return pkg_name[:_get_java_root_index(pkg_name)]

def get_java_package(pkg_name):
  """Extract the java package from the build package"""
  return pkg_name[len(get_java_root(pkg_name)):].replace("/", ".")

"""j2cl_import_from_gwt build rule

Creates a j2cl_library target from existing GWT source by automatically
overlaying files from current directory.

"""

load(":j2cl_source_copy.bzl", "j2cl_source_copy")
load(":jsni_to_j2cl_converter.bzl", "jsni_to_native_js_bundle")
load("//third_party/java/j2cl:j2cl_library.bzl", "j2cl_library")

def j2cl_mirror_from_gwt(name, mirrored_files, deps=[], js_deps=[], **kwargs):

  super_srcs = native.glob(["**/*.java"])
  native_srcs = native.glob(["**/*.native.js"])
  js_srcs = native.glob(["**/*.js"], exclude = native_srcs)
  omitted_srcs = [f.replace(".js", ".java") for f in js_srcs if not f.endswith(".impl.js")]

  j2cl_source_copy(
      name = name + "_copy",
      srcs = mirrored_files,
      excludes = super_srcs,
  )

  native.filegroup(
      name = name + "_files",
      srcs = [":" + name + "_copy"] + super_srcs,
  )

  jsni_to_native_js_bundle(
      name = name +"_native_zips",
      srcs = [":" + name + "_files"],
      native_srcs = native_srcs,
      testonly = kwargs.get("testonly", 0),
      deps = [":" + name + "_java_library"],
  )

  native.js_library(
      name = name + "_handrolled_js",
      srcs = js_srcs,
  )

  j2cl_library(
      name = name,
      srcs = [":" + name + "_files"],
      native_srcs_zips = [":" + name + "_native_zips"],
      deps = deps,
      _js_deps = js_deps + [":" + name + "_handrolled_js"],
      _omit_srcs = omitted_srcs,
      testonly = kwargs.get("testonly", 0),
      **kwargs
  )

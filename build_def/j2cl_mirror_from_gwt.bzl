"""j2cl_mirror_from_gwt build rule

Creates a j2cl_library target from existing GWT source by automatically
overlaying files from current directory.

"""

load(":j2cl_source_copy.bzl", "j2cl_source_copy")
load(":j2cl_library.bzl", "j2cl_library")
load(":j2cl_util.bzl", "generate_zip")

def j2cl_mirror_from_gwt(name,
                         mirrored_files,
                         extra_srcs=[],
                         extra_js_srcs=[],
                         deps=[],
                         js_deps=[],
                         **kwargs):

  super_srcs = native.glob(["**/*.java"]) + extra_srcs
  native_srcs = native.glob(["**/*.native.js"])
  js_srcs = native.glob(["**/*.js"], exclude = native_srcs) + extra_js_srcs

  j2cl_source_copy(
      name = name + "_copy",
      srcs = mirrored_files,
      excludes = super_srcs,
  )

  native.filegroup(
      name = name + "_java_files",
      srcs = [":" + name + "_copy"] + super_srcs,
  )

  # TODO(b/67481861): Do we really need custom zipping w/ RELATIVE?
  generate_zip(
      name = name + "_native.zip",
      srcs = native_srcs,
      pkg = "RELATIVE",
  )

  j2cl_library(
      name = name,
      srcs = [":" + name + "_java_files"],
      native_srcs_zips = [name + "_native.zip"],
      deps = deps,
      _js_srcs = js_srcs,
      _js_deps = js_deps,
      **kwargs
  )


"""j2cl_mirror_from_gwt build rule

Creates a j2cl_library target from existing GWT source by automatically
overlaying files from current directory.

"""

load(":j2cl_source_copy.bzl", "j2cl_source_copy")
load(":jsni_to_native_js_bundle.bzl", "jsni_to_native_js_bundle")
load("//third_party/java/j2cl:j2cl_library.bzl", "j2cl_library")

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

  jsni_to_native_js_bundle(
      name = name +"_native_zips",
      srcs = [":" + name + "_java_files"],
      native_srcs = native_srcs,
      testonly = kwargs.get("testonly", 0),
      deps = [":" + name + "_java_library"],
  )

  packaged_js_srcs = None
  if js_srcs:
    native.filegroup(
        name = name + "_js_files",
        srcs = js_srcs,
    )
    packaged_js_srcs = [":" + name + "_js_files"]

  j2cl_library(
      name = name,
      srcs = [":" + name + "_java_files"],
      _js_srcs = packaged_js_srcs,
      native_srcs_zips = [":" + name + "_native_zips"],
      deps = deps,
      _js_deps = js_deps,
      **kwargs
  )


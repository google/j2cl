#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""j2cl_transpile build rule.

This build extension defines a new rule j2cl_transpile, that takes a
java_library as input and emits JavaScript for each Java file from the
java_library. Since skylark does not allow access to native providers yet, one
still has to list srcs and dependencies manually.

Here is an example use of j2cl_transpile:

java_library(
   name = "my_java_library",
   srcs = ["MyJavaFile.java"],
   deps = [":my_deps"],
)

j2cl_transpile(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    java_library = ":my_java_library",
    java_deps = [":my_deps"],
)

Note: in general you want to be using j2cl_java_library instead of using
j2cl_transpile directly.

"""

load("//build_def/j2cl_util", "get_java_root")

def _impl(ctx):
  """Implementation for j2cl_transpile"""
  java_files = ctx.files.srcs # java files that need to be compiled
  java_deps = ctx.files.java_deps
  java_deps_paths = []
  java_files_paths = []
  js_files = []
  js_files_paths = []

  # base package for the build
  package_name = ctx.label.package

  for java_dep in java_deps:
    java_deps_paths += [java_dep.path]

  java_root = get_java_root(package_name)
  index = 0
  for java_file in java_files:
    # the java file path is absolute therefore the javascript one is as well
    js_file_name = java_file.path[:-len("java")] + "js"

    # make js file relative to the build package
    base_js_file_name = js_file_name[len(package_name) + 1:]

    # create new output file
    js_file_artifact = ctx.new_file(base_js_file_name)
    js_files += [js_file_artifact]
    # cut off the base build package since this is included in the output dir
    # the compiler itself only outputs packages.
    js_files_paths += [js_file_artifact.path[len(java_root):]]

    java_files_paths += [java_file.path]
    index +=1

  compiler_args = [
    "-d",
    ctx.configuration.bin_dir.path + "/" + java_root,
  ]

  if len(java_deps_paths) > 0:
    compiler_args += ["-cp", ",".join(java_deps_paths)]

  compiler_args += [",".join(java_files_paths)]

  ctx.action(
      inputs=java_files + java_deps,
      outputs=js_files,
      executable = ctx.executable.compiler,
      arguments = compiler_args,
  )

  # We need to return the output files so that they get recognized as outputs
  # from blaze
  return struct(
      files=set(js_files),
  )

# expose rule
j2cl_transpile = rule(
    attrs = {
        "java_library": attr.label(mandatory = True),
        "java_deps": attr.label_list(
            allow_files = FileType([".jar"]),
        ),
        "compiler": attr.label(
            cfg = HOST_CFG,
            executable = True,
            allow_files = True,
            default = Label("//:j2cl"),
        ),
        # these need to go once we know how to read the inputs of
        # a java_library rule
        "srcs": attr.label_list(
            mandatory = True,
            allow_files = FileType([".java"]),
        ),
    },
    implementation = _impl,
)

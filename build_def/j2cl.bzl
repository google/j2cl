#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""J2Cl build macros and rules.
(j2cl_transpile, j2cl_java_library)

j2cl_transpile:
This build extension defines a new rule j2cl_transpile, that takes a
java_library as input and emits JavaScript for each Java file from the
java_library. Since skylark does not allow access to native providers yet, one
still has to list srcs and dependencies manually.

j2cl_java_library:
A build macro that defines a java_library rule, a j2cl_transpile rule that
transpiles to JavaScript and a js_library referencing compiled output.
"""

def _impl(ctx):
  """Implementation  for j2cl_transpile"""
  java_files = ctx.files.srcs # java files that need to be compiled
  java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  for java_file in java_files:
    # the java file path is absolute therefore the javascript one is as well
    js_file_name = java_file.path[0: -len("java")] + "js"

    # make js file relative to the build package
    base_js_file_name = js_file_name[len(package_name) + 1:]

    # create new output file
    js_file_artifact = ctx.new_file(base_js_file_name)
    js_files += [js_file_artifact]

    java_files_paths += [java_file.path]

  compiler_args = [
    "-out",
    ctx.configuration.bin_dir.path,
    "-deps",
    "asdf.jar",
    "-i",
    ",".join(java_files_paths),
  ]

  ctx.action(
      inputs=java_files,
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
            allow_files = True,
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

def j2cl_java_library(**kwargs):
  """A macro that emits j2cl_transpile, java_library and js_library rules."""
  native.java_library(**kwargs)

  java_deps = []
  js_deps = []

  if "deps" in kwargs:
    for dep in kwargs["deps"]:
      java_deps += [dep]
      js_deps += [dep + "_js"]

  j2cl_transpile(
      name = kwargs["name"]  + "_j2cl_transpile",
      srcs = kwargs["srcs"],
      java_library = ":" + kwargs["name"],
      java_deps = java_deps,
  )
  native.js_library(
      name = kwargs["name"]  + "_js_library",
      srcs = [":" + kwargs["name"]  + "_j2cl_transpile"],
      deps = js_deps,
  )

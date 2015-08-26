"""j2cl_transpile_zip build rule.

This build extension defines a new rule j2cl_transpile_zip, that takes a
java_library as input and emits a bundle zip of JavaScript transpiled from
the Java files in the java_library. Since skylark does not allow access to
native providers yet, one still has to list srcs and dependencies manually.

Here is an example use of j2cl_transpile_zip:

java_library(
   name = "my_java_library",
   srcs = ["MyJavaFile.java"],
   deps = [":my_deps"],
)

j2cl_transpile_zip(
    name = "my_transpile",
    srcs = ["MyJavaFile.java"],
    java_library = ":my_java_library",
    java_deps = [":my_deps"],
)

Note: in general you want to be using j2cl_java_library instead of using
j2cl_transpile_zip directly.

"""

load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_root")


# TODO: replace j2cl_transpile.bzl when Blaze is ready.
# TODO: update python scripts to use this path.
def _impl(ctx):
  """Implementation for j2cl_transpile_zip"""
  java_files = ctx.files.srcs  # java files that need to be compiled
  super_java_files = ctx.files.super_srcs  # java files whose js to ignore
  java_deps = ctx.files.java_deps
  java_deps_paths = []
  java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  for java_dep in java_deps:
    java_deps_paths += [java_dep.path]

  for java_file in java_files:
    java_files_paths += [java_file.path]

  js_zip_name = ctx.label.name + ".js.zip"
  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name,]

  if len(java_deps_paths) > 0:
    host_path_separator = ctx.configuration.host_path_separator
    compiler_args += ["-cp", host_path_separator.join(java_deps_paths)]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  js_zip_artifact = ctx.new_file(js_zip_name)
  ctx.action(
      inputs=java_files + java_deps,
      outputs=[js_zip_artifact],
      executable=ctx.executable.compiler,
      arguments=compiler_args,
  )

  return struct(
      files=set([js_zip_artifact]),
  )

# expose rule
j2cl_transpile_zip = rule(
    attrs={
        "java_library": attr.label(mandatory=True),
        "java_deps": attr.label_list(
            allow_files=FileType([".jar"]),
        ),
        "compiler": attr.label(
            cfg=HOST_CFG,
            executable=True,
            allow_files=True,
            default=Label("//:j2cl"),
        ),
        # these need to go once we know how to read the inputs of
        # a java_library rule
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=FileType([".java"]),
        ),
        "super_srcs": attr.label_list(
            allow_files=FileType([".java"]),
        ),
    },
    implementation=_impl,
)

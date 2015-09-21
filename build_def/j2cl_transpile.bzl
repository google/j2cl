"""j2cl_transpile build rule.

This build extension defines a new rule j2cl_transpile, that takes a
java_library as input and emits a bundle zip of JavaScript transpiled from
the Java files in the java_library. Since skylark does not allow access to
native providers yet, one still has to list srcs and dependencies manually.

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

load("/third_party/java_src/j2cl/build_def/j2cl_util", "get_java_root")


def _impl(ctx):
  """Implementation for j2cl_transpile"""
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  super_java_files = ctx.files.super_srcs  # java files whose js to ignore
  java_deps = ctx.files.java_deps
  java_deps_paths = []
  java_files_paths = []
  super_java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  for java_dep in java_deps:
    java_deps_paths += [java_dep.path]

  for java_file in java_files:
    if java_file in super_java_files:
      super_java_files_paths += [java_file.path]
    java_files_paths += [java_file.path]

  # TODO: use .js.zip extension when Blaze allows it.
  js_zip_name = ctx.label.name + ".pintozip"
  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name,]

  if len(java_deps_paths) > 0:
    compiler_args += ["-cp", separator.join(java_deps_paths)]

  if len(super_java_files_paths) > 0:
    compiler_args += ["-superfiles", separator.join(super_java_files_paths)]

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
j2cl_transpile = rule(
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

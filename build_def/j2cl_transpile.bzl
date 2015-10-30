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

def _is_in_super(java_file, super_srcs):
  for super_src in super_srcs:
    if java_file.path.endswith(super_src):
      return True
  return False;

def _impl(ctx):
  """Implementation for j2cl_transpile"""
  separator = ctx.configuration.host_path_separator
  java_files = ctx.files.srcs  # java files that need to be compiled
  super_java_files = ctx.attr.super_srcs  # java files whose js to ignore
  js_native_zip_files = ctx.files.native_sources_zips
  java_deps = ctx.attr.java_deps
  java_dep_files = set()
  java_deps_paths = []
  java_files_paths = []
  super_java_files_paths = []
  js_files = []

  # base package for the build
  package_name = ctx.label.package

  # gather transitive files and exported files in deps
  for java_dep in java_deps:
    java_dep_files += java_dep.files
    java_dep_files += java_dep.default_runfiles.files # for exported libraries

  # convert files to paths
  for java_dep_file in java_dep_files:
    java_deps_paths += [java_dep_file.path]

  for java_file in java_files:
    if _is_in_super(java_file, super_java_files):
      super_java_files_paths += [java_file.path]
    java_files_paths += [java_file.path]

  js_zip_name = ctx.label.name + ".js.zip"
  compiler_args = [
      "-d",
      ctx.configuration.bin_dir.path + "/" + ctx.label.package + "/" +
      js_zip_name,]

  if len(java_deps_paths) > 0:
    compiler_args += ["-cp", separator.join(java_deps_paths)]

  if len(super_java_files_paths) > 0:
    compiler_args += ["-superfiles", separator.join(super_java_files_paths)]

  # Add the native zip file paths
  js_native_zip_files_paths = [js_native_zip_file.path for js_native_zip_file
                               in js_native_zip_files]
  if js_native_zip_files_paths:
    joined_paths = separator.join(js_native_zip_files_paths)
    compiler_args += ["-nativesourcezip", joined_paths]

  # The transpiler expects each java file path as a separate argument.
  compiler_args += java_files_paths

  js_zip_artifact = ctx.new_file(js_zip_name)
  ctx.action(
      inputs=java_files + list(java_dep_files) + js_native_zip_files,
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
        "super_srcs": attr.string_list(default=[]),
        "native_sources_zips": attr.label_list(
            allow_files=FileType([".zip"]),
        )
    },
    implementation=_impl,
)

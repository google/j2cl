"""jsni_to_j2cl_converter build rule

Takes java files with old GWT JSNI methods and generates J2CL Foo.native.js
files from them.


Example use:

jsni_to_native_js_bundle(
    name = "foo_lib_native_js",
    srcs = glob(["*.java"]),
    native_srcs = glob(["*.native.js"]),
)

which automatically excludes the files that a native.js file already provided
and generates a single bundle together with auto generated natve.js files.

"""

load(":j2cl_util.bzl", "generate_zip")


def _impl(ctx):
  java_files = ctx.files.srcs
  exclude_files = [f for f in java_files
                   if any([f.path.endswith(x) for x in ctx.attr.excludes])]
  dep_targets = ctx.attr.deps
  zip_file = ctx.new_file(ctx.label.name + "_native.js.zip")  # output zip file

  if ctx.attr.debug:
    print("java files: " + str(java_files))
    print("dep_targets: " + str(dep_targets))
    print("zip result file: " + str(zip_file))

  converter_args = ["--output_file", zip_file.path]
  if ctx.attr.debug:
    converter_args += ["--verbose"]
  for exclude_file in exclude_files:
    converter_args += ["--excludes", exclude_file.path]
  dep_files = set()
  for dep_target in dep_targets:
    dep_files += dep_target.files
    dep_files += dep_target.default_runfiles.files  # for exported libraries
  for dep_file in dep_files:
    converter_args += ["--class_path", dep_file.path]
  converter_args += [f.path for f in java_files]

  ctx.action(
      inputs=java_files + list(dep_files),
      outputs=[zip_file],
      executable=ctx.executable.converter_tool,
      progress_message="Converting jsni methods into native js files",
      arguments=converter_args,
  )

  return struct(
      files=set([zip_file]),
  )


_jsni_to_j2cl_converter = rule(
    attrs={
        "srcs": attr.label_list(
            mandatory=True,
            allow_files=[".java"],
        ),
        "debug": attr.bool(default=False),
        # Uh-oh: jsni_to_j2cl_converter needs to depend on
        # restricted_to=j2cl_compilation targets. But the jsni_to_j2cl_converter
        # can't (or at least probably shouldn't?) itself be
        # restricted-to=j2cl_compilation. Our solution is to defeat the
        # constraints system by building the dependencies as host dep. For more
        # information, see the similar comment in
        # third_party/java/j2cl/j2cl_library.bzl
        # TODO(cpovirk): Find a less evil solution, maybe based on
        # http://b/27044764
        "deps": attr.label_list(
            cfg="host",
            allow_files=FileType([".jar"]),
        ),
        "excludes": attr.string_list(default=[]),
        "converter_tool": attr.label(
            cfg="host",
            allow_files=True,
            executable=True,
            default=Label("//third_party/java/j2cl/internal_do_not_use:JsniConverter")
        )
    },
    implementation=_impl,
)

def jsni_to_native_js_bundle(name, srcs, native_srcs=[], testonly = 0, **kwargs):

  _jsni_to_j2cl_converter(
     name = name + "_autogen",
     srcs = srcs,
     excludes = [n.replace(".native.js", ".java") for n in native_srcs],
     testonly = testonly,
      **kwargs
  )

  generate_zip(
      name = name + "_handrolled.js.zip",
      srcs = native_srcs,
      pkg = "RELATIVE",
      testonly = testonly,
  )

  native.filegroup(
      name = name,
      srcs = [":" + name + "_autogen", ":" + name + "_handrolled.js.zip"],
      testonly = testonly,
  )

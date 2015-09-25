"""j2cl_create_zip build rule.

This build extension defines a new rule j2cl_create_zip, that zip
javascript files.

Here is an example use of j2cl_create_zip:

j2cl_create_zip(
   name = "my_zip",
   js = ["my/javascript/file.js"],
)

"""


def _impl(ctx):
  """Implementation for j2cl_create_zip"""
  # The whole implementation looks a little weird:
  # Apparently there is no way to do relative paths with zip ignoring part of the original path
  # this is why we end up copying files around so we can use the relative file in regards to the
  # zip output file.
  js_files = ctx.files.js
  zip_output_name = ctx.label.name
  output_dir = ctx.configuration.bin_dir.path + "/" + ctx.attr.base_path + "/"
  script = ""
  zip_file_artifact = ctx.new_file("%s.zip" % zip_output_name)

  first_command = True

  # copy javascript files to out
  for js_file in js_files:
    base_dir = js_file.path[
        len(ctx.attr.base_path) + 1: js_file.path.rfind("/") + 1]
    if first_command:
      first_command = False
    else:
      script += "&& "
    # create a new relative dir
    script += "mkdir -p %s " % (ctx.configuration.bin_dir.path + "/"
                                   + ctx.attr.base_path + "/" + base_dir)
    # copy the JavaScript file into it
    script += "&& cp %s %s " % (js_file.path, ctx.configuration.bin_dir.path
                                + "/" + ctx.attr.base_path + "/" + base_dir)

  # cd into the directory that has the zip file
  script += " && cd %s " % (ctx.configuration.bin_dir.path + "/" +
                            ctx.attr.base_path)

  for js_file in js_files:
    # start zipping files into it
    js_file_relative = js_file.path[len(ctx.attr.base_path) + 1:]
    script += " && zip -q %s %s " % (zip_output_name, js_file_relative)

  ctx.action(
      inputs=ctx.files.js,
      outputs=[zip_file_artifact],
      command=script,
      arguments=[],
  )
  # We need to return the output files so that they get recognized as outputs
  # from blaze
  return struct(
      files=set([zip_file_artifact]),
  )


_j2cl_create_zip = rule(
    attrs={
        "js": attr.label_list(
            mandatory=True,
            allow_files=FileType([".js"]),
        ),
        "base_path": attr.string(
            default="",
        ),
    },
    implementation=_impl,
)

# expose rule
def j2cl_create_zip(name, js):
  _j2cl_create_zip(
      name=name,
      js=js,
      base_path=PACKAGE_NAME,
  )

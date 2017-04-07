"""Utility functions for templated primitive class creation."""


def generate_primitive_type(name, **kwargs):
  """Generates src for J2CL primitive types"""

  # exit early to avoid parse errors when running under bazel
  if not hasattr(native, "js_library"):
    return

  templates = ["primitive.java.js", "primitive.impl.java.js"]
  native.filegroup(
      name="src_" + name,
      srcs=[_gen_src_from_template(t, name, **kwargs) for t in templates])


def _gen_src_from_template(template,
                           name,
                           shortName,
                           jsTypeName="number",
                           initValue="0"):

  targetName = "src_" + name + template.replace(".", "_")

  native.genrule(
      name=targetName,
      srcs=[template + ".template"],
      outs=[template.replace("primitive", name)],
      message="Generating sources for primitives",
      cmd="echo '// GENERATED CODE! Edit $(SRCS) instead!' > $@ && " +
      "cat $(SRCS) " + "| sed -e 's/%PRIMITIVE_NAME%/" + name + "/g' " +
      "| sed -e 's/%PRIMITIVE_SHORT_NAME%/" + shortName + "/g' " +
      "| sed -e 's/%PRIMITIVE_JS_TYPE%/" + jsTypeName + "/g' " +
      "| sed -e 's/%PRIMITIVE_INIT_VALUE%/" + initValue + "/g' " +
      "| sed -e 's/%" + name + "_ONLY% //g' " + "| sed -e '/_ONLY%/d' " +
      ">> $@",)

  return targetName

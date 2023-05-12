"""Functions for templated WasmArray type creation."""

def generate_wasm_array(name, **kwargs):
    """Generates src for WasmArray"""

    native.filegroup(
        name = "src_" + name,
        srcs = [_gen_src_from_template("WasmArray.java", name, **kwargs)],
    )

def _gen_src_from_template(
        template,
        name,
        subtypes):
    target_name = "src_" + name + template.replace(".", "_")
    template_name = template + ".template"
    subtype_files = ["%s.java" % subtype for subtype in subtypes]

    native.genrule(
        name = target_name,
        srcs = [template_name] + subtype_files,
        outs = [template],
        message = "Generating sources for WasmArray",
        cmd = "echo '// GENERATED CODE! Edit %s instead!' > $@ && " % template_name +
              "cat $(location %s) " % template_name +
              "".join(
                  ["| sed -e '/%%SUBTYPES%%/r $(location %s)' " % f for f in subtype_files],
              ) + " -e '//d ' " +
              ">> $@",
    )

    return target_name

def generate_wasm_array_subtype(name, **kwargs):
    """Generates src for WasmArray.OfType"""

    native.filegroup(
        name = "src_" + name,
        srcs = [_gen_subtype_src_from_template("OfType.java", name, **kwargs)],
    )

def _gen_subtype_src_from_template(
        template,
        name,
        elementTypeName,
        elementDefaultValue):
    target_name = "src_" + name + template.replace(".", "_")

    native.genrule(
        name = target_name,
        srcs = [template + ".template"],
        outs = [template.replace("OfType", name)],
        message = "Generating sources for WasmArray.%s" % name,
        cmd = "echo '  // GENERATED CODE! Edit $(SRCS) instead!' > $@ && " +
              "cat $(SRCS) " +
              "| sed -e 's/%ARRAY_TYPE_NAME%/" + name + "/g' " +
              "| sed -e 's/%TYPE_NAME%/" + elementTypeName + "/g' " +
              "| sed -e 's/%DEFAULT_VALUE%/" + elementDefaultValue + "/g' " +
              # Exclude or include type-specific sections
              # For example, on generating OfObject, we include code between:
              #   #IF OfObject
              #     ... include ...
              #   #ENDIF
              # And exclude:
              #   #IF OfInt OfLong
              #     ... exclude ...
              #   #ENDIF
              "| awk '" +
              "  /^#IF[a-zA-Z ]* %s( |$$)/ { exclude = 0; next; } " % name +
              "  /^#IF[a-zA-Z ]*$$/ { exclude = 1; next; } " +
              "  /^#ENDIF$$/ { exclude = 0; next; } " +
              "  !exclude " +
              "' " +
              ">> $@",
    )

    return target_name

"""Stub implementation for J2CL RTA utilities not needed in open-source.
"""
RTA_ASPECT_ATTRS = ["deps", "exports", "module_deps", "modules"]

def get_aspect_providers():
    return []

def get_j2cl_info_from_aspect_providers():
    return None

def _pmf_file_aspect_impl(target, ctx):
    return []

pmf_file_aspect = aspect(
    attr_aspects = RTA_ASPECT_ATTRS,
    implementation = _pmf_file_aspect_impl,
)

def write_module_names_file(ctx):
    ctx.actions.run_shell(
        outputs = [ctx.outputs.module_name_list],
        command = "touch \"%s\"" % ctx.outputs.module_name_list.path,
    )

"""Stub implementation containing j2kt_provider helpers."""

def _to_j2kt_name(name):
    """Convert a label name used in j2cl to be used in j2kt"""
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2kt" % name

def _compile(**kwargs):
  pass

j2kt_common = struct(
    to_j2kt_name = _to_j2kt_name,
    compile = _compile,
)

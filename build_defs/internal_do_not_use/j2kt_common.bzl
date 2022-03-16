"""Stub implementation containing j2kt_provider helpers."""

def _to_j2kt_jvm_name(name):
    """Convert a label name used in j2cl to be used in j2kt jvm"""
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2kt-jvm" % name

def _to_j2kt_native_name(name):
    """Convert a label name used in j2cl to be used in j2kt native"""
    if name.endswith("-j2cl"):
        name = name[:-5]
    return "%s-j2kt-native" % name

def _compile(**kwargs):
    pass

def _native_compile(**kwargs):
    pass

def _jvm_compile(**kwargs):
    pass

j2kt_common = struct(
    to_j2kt_jvm_name = _to_j2kt_jvm_name,
    to_j2kt_native_name = _to_j2kt_native_name,
    compile = _compile,
    native_compile = _native_compile,
    jvm_compile = _jvm_compile,
)

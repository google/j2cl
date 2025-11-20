"""Utilities for klibs pipeline."""

def _is_klibs_experiment_enabled(ctx):
    return False

def _compile_header_klibs(**kwargs):
    return None

def _create_klib_info_for_import(java_info):
    return None

klib_common = struct(
    is_klibs_experiment_enabled = _is_klibs_experiment_enabled,
    compile_header_klibs = _compile_header_klibs,
    create_klib_info_for_import = _create_klib_info_for_import,
)

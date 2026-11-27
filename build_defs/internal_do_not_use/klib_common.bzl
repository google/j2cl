"""Utilities for klibs pipeline."""

def _compile_header_klibs(**kwargs):
    return None

def _create_klib_info_for_import(java_info):
    return None

def _get_compilation_inputs(klib_deps):
    return (depset(), depset())

def _get_klib_friends(ctx, deps):
    return depset()

def _get_klib_infos(deps):
    return []

klib_common = struct(
    compile_header_klibs = _compile_header_klibs,
    create_klib_info_for_import = _create_klib_info_for_import,
    get_compilation_inputs = _get_compilation_inputs,
    get_klib_friends = _get_klib_friends,
    get_klib_infos = _get_klib_infos,
)

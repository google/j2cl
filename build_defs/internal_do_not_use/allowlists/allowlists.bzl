"""Utilities for creating and applying allowlists."""

visibility(["//build_defs/internal_do_not_use/..."])

def _is_package_in_allowlist(package, allowlist):
    """Returns whether the given package is in the allowlist.

    Args:
      package: a str, the package to check is in the allowlist.
      allowlist: an allowlist created via allowlists.of_packages()

    Returns:
      True if package is in the allowlist, else False.
    """
    _check_is_allowlist_of_type(allowlist, "package")

    package = _normalize_package(package)

    for entry in allowlist.entries:
        if package == entry or _is_subpackage_of(package, entry):
            return True
    return False

def _is_target_in_allowlist(target, allowlist):
    """Returns whether the given target is in the allowlist.

    Args:
      target: a str or Label, the target to check is in the allowlist.
      allowlist: an allowlist created via make_target_allowlist

    Returns:
      True if target is in the allowlist, else False.
    """
    _check_is_allowlist_of_type(allowlist, "target")

    return _as_label(target) in allowlist.entries

def _check_is_allowlist_of_type(allowlist, allowlist_type):
    if type(allowlist) == "struct" and allowlist._j2cl_allowlist_type == allowlist_type:
        return
    create_function = "of_targets" if allowlist_type == "target" else "of_packages"
    fail("Improper allowlist, was it created with allowlists.%s()?" % create_function)

def _make_package_allowlist(packages, include = []):
    """Returns an allowlist struct configured for package matching.

    Args:
      packages: a list of str, the packages to add to the allowlist. Package
        paths may end with '/...' to allow for subpackage matching.
      include: a list of allowlist structs, their entries will be added to this
        allowlist.

    Returns:
      An allowlist struct configured for package matching.
    """
    entries = [_check_package_definition(p) for p in packages]

    for allowlist in include:
        _check_is_allowlist_of_type(allowlist, "package")
        entries += allowlist.entries

    return struct(
        _j2cl_allowlist_type = "package",
        entries = entries,
    )

def _make_target_allowlist(targets, include = []):
    """Returns an allowlist struct configured for target matching.

    Args:
      targets: a list of (str or Label), the targets to add to the allowlist.
      include: a list of allowlist structs, their entries will be added to this
        allowlist.
    Returns:
      An allowlist struct configured for target matching.
    """

    entries = [_as_label(t) for t in targets]

    for allowlist in include:
        _check_is_allowlist_of_type(allowlist, "target")
        entries += allowlist.entries

    return struct(
        _j2cl_allowlist_type = "target",
        entries = entries,
    )

def _as_label(target):
    if type(target) == "Label":
        return target
    return Label(target)

def _normalize_package(package):
    if not package.startswith("//"):
        package = "//" + package
    return package

def _check_package_definition(package):
    if not package.startswith("//"):
        fail("Packages should start with //, but '%s' does not." % package)

    if package.endswith("/"):
        fail("Packages should not end with /, but '%s' does." % package)

    if ":" in package:
        fail("Packages should not include a label, but '%s' does." % package)

    return package

def _is_subpackage_of(package, target_package):
    # If the target package doesn't allow subpackage matching, return false.
    if not target_package.endswith("/..."):
        return False

    # Trim of the trailing /... and see if it's an exact match.
    if package == target_package[:-4]:
        return True

    # Trim off the trailing ... and see if the target is a prefix match.
    return package.startswith(target_package[:-3])

allowlists = struct(
    of_packages = _make_package_allowlist,
    of_targets = _make_target_allowlist,
    is_package_allowed = _is_package_in_allowlist,
    is_target_allowed = _is_target_in_allowlist,
)

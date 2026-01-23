"""Utilities for creating and applying allowlists."""

visibility(["//build_defs/internal_do_not_use/..."])

def _make_package_allowlist(packages, include = [], exclude = []):
    """Returns an allowlist struct configured for package matching.

    Args:
      packages: a list of str, the packages to add to the allowlist. Package
        paths may end with '/...' to allow for subpackage matching.
      include: a list of allowlist structs, their entries will be added to this
        allowlist.

    Returns:
      An allowlist struct configured for package matching.
    """
    return _make_allowlist(
        check_fn = _is_or_subpackage_of,
        entries = [_check_package_definition(p) for p in packages],
        include = include,
        exclude = exclude,
    )

def _make_target_allowlist(targets, include = [], exclude = []):
    """Returns an allowlist struct configured for target matching.

    Args:
      targets: a list of (str or Label), the targets to add to the allowlist.
      include: a list of allowlist structs, their entries will be added to this
        allowlist.
    Returns:
      An allowlist struct configured for target matching.
    """

    return _make_allowlist(
        check_fn = lambda label, entry: label == entry,
        entries = [_as_label(t) for t in targets],
        include = include,
        exclude = exclude,
    )

def _make_allowlist(check_fn, entries, include, exclude):
    """Returns an allowlist struct configured for the given check and entries."""

    def _accepts(target):
        label = _as_label(target)
        for e in entries:
            if check_fn(label, e):
                return True
        for i in include:
            if i.accepts(label):
                return True
        return False

    def _rejects(target):
        label = _as_label(target)
        for e in exclude:
            if e.accepts(label):
                return True
        return False

    # Partial workaround to https://github.com/bazelbuild/bazel/issues/9163 via two lambdas.
    if exclude:
        fn = lambda label: not _rejects(label) and _accepts(label)
    else:
        fn = _accepts
    return struct(accepts = fn)

def _as_label(target):
    if type(target) == "Label":
        return target
    return Label(target)

def _check_package_definition(package):
    if not package.startswith("//"):
        fail("Packages should start with //, but '%s' does not." % package)

    if package.endswith("/"):
        fail("Packages should not end with /, but '%s' does." % package)

    if ":" in package:
        fail("Packages should not include a label, but '%s' does." % package)

    return package

def _is_or_subpackage_of(label, target_package):
    package = "//" + label.package

    # If the target package is an exact match, return true.
    if package == target_package:
        return True

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
)

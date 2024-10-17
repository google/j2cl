"""Utility functions for the j2cl_* build rules / macros"""

def get_java_package(path):
    """Extract the java package from path

    Finds the smallest path inside a java class eg:
      java/com/foo/bard/javatests/com/google/java/emptyclass will return
      emptyclass.

    Args:
        path: Path to extra the java package from.
    Returns:
        Java package name (path from last java or javatest separated by a '.')
    """

    return ".".join(_get_java_segments(path))

def get_java_path(path):
    """Extract the path to the java package from path

    Finds the smallest path inside a java class eg:
      java/com/foo/bard/javatests/com/google/java/emptyclass will return
      emptyclass.

    Args:
        path: Path to extra the java package from.
    Returns:
        Path to java package (path from last java or javatest separated by a '/')
    """

    return "/".join(_get_java_segments(path))

def _get_java_segments(path):
    segments = path.split("/")

    # Find different root start indices based on potential java roots.
    java_root_start_indices = [_find(segments, root) for root in ["java", "javatests"]]

    # Choose the root that starts latest.
    start_index = max(java_root_start_indices)

    kotlin_start_index = _find(segments, "kotlin")

    if start_index >= 0:
        return segments[start_index + 1:]
    elif kotlin_start_index >= 0:
        return segments[kotlin_start_index + 1:]

    # return segments as workspace-rooted if no matches are found from source root
    return segments

def _find(segments, s):
    for i in reversed(range(len(segments))):
        if segments[i] == s:
            return i

    return -1

def to_parallel_targets(key, args, name_fun):
    labels = args.get(key)
    if not labels:
        return

    args[key] = [to_parallel_target(label, name_fun) for label in labels]

def to_parallel_target(label, name_fun):
    if type(label) == "string":
        return name_fun(absolute_label(label))

    # Label Object
    return label.relative(":%s" % name_fun(label.name))

def absolute_label(label):
    if label.startswith("//") or label.startswith("@"):
        if ":" in label:
            return label
        elif "/" in label:
            return "%s:%s" % (label, label.rsplit("/", 1)[-1])
        if not label.startswith("@"):
            fail("Unexpected label format: %s" % label)
        return "%s//:%s" % (label, label[1:])

    package_name = native.package_name()

    if label.startswith(":"):
        return "//%s%s" % (package_name, label)
    if ":" in label:
        return "//%s/%s" % (package_name, label)
    return "//%s:%s" % (package_name, label)

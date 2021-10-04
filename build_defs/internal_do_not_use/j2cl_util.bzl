"""Utility functions for the j2cl_* build rules / macros"""

def get_java_package(path):
    """Extract the java package from path

    Finds the smallest path inside a java class eg:
      java/com/foo/bard/javatests/com/google/java/emptyclass will return
      emptyclass.

    Args:
        path: Path to extra the java package from.
    Returns:
        Java package name (path from last java or javatest sperated by a '.')
    """

    segments = path.split("/")

    # Find different root start indecies based on potential java roots.
    java_root_start_indecies = [_find(segments, root) for root in ["java", "javatests"]]

    # Choose the root that starts latest.
    start_index = max(java_root_start_indecies)

    if start_index < 0:
        fail("Cannot find java root: " + path)

    return ".".join(segments[start_index + 1:])

def _find(segments, s):
    for i in reversed(range(len(segments))):
        if segments[i] == s:
            return i

    return -1

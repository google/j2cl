"""Utility functions for the j2cl_* build rules / macros"""

def get_java_package(path):
    """Extract the java package from path"""

    segments = path.split("/")

    # Find different root start indecies based on potential java roots
    java_root_start_indecies = [_find(segments, root) for root in ["java", "javatests"]]

    # Choose the root that starts earliest
    start_index = min(java_root_start_indecies)

    if start_index == len(segments):
        fail("Cannot find java root: " + path)

    return ".".join(segments[start_index + 1:])

def _find(segments, s):
    return segments.index(s) if s in segments else len(segments)

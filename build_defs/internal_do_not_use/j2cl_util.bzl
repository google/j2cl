"""Utility functions for the j2cl_* build rules / macros"""

visibility([
    "//benchmarking/java/com/google/j2cl/benchmarking",
    "//build_defs/...",
    "//junit/generator/javatests/com/google/j2cl/junit/integration",
    "//transpiler/javatests/com/google/j2cl/integration",
])

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

    # 1. Match standard Maven/Gradle source layout: src/(main|test)/(java|javatests|kotlin)
    for i in range(len(segments) - 1, -1, -1):
        s = segments[i]
        if s == "java" or s == "javatests" or s == "kotlin":
            if i >= 2 and segments[i - 2] == "src" and (segments[i - 1] == "main" or segments[i - 1] == "test"):
                return segments[i + 1:]
            if i >= 1 and (segments[i - 1] == "src" or segments[i - 1] == "testsrc"):
                return segments[i + 1:]

    # 2. Check for 'javatests' (test source roots in Google/Bazel monorepos)
    javatests_indices = [i for i in range(len(segments)) if segments[i] == "javatests"]
    if javatests_indices:
        return segments[javatests_indices[-1] + 1:]

    # 3. Find the earliest 'java' or 'kotlin' root (e.g. java/com/... or kotlin/com/...)
    for i in range(len(segments)):
        if segments[i] == "java" or segments[i] == "kotlin":
            return segments[i + 1:]

    # Return segments as workspace-rooted if no matches are found from source root
    return segments

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

"""Rules for defining external J2CL dependencies.

This rules is analogous to [java_import_external]
(https://github.com/bazelbuild/bazel/blob/master/tools/build_defs/repo/java.bzl)
to define external J2CL dependencies.

"""

# TODO(b/71517767): This could a simple wrapper around java_import_external if
# j2cl_import supports source jars.

_PASS_PROPS = (
    "testonly_",
    "visibility",
    "exports",
    "deps",
    "tags",
)

def _j2cl_import_external(repository_ctx):
    """Implementation of `j2cl_import_external` rule."""

    lines = [
        "load('@com_google_j2cl//build_defs:rules.bzl', 'j2cl_library', 'j2cl_import')",
        "",
        "package(default_visibility = %s)" % repository_ctx.attr.default_visibility,
        "",
        "licenses(%s)" % repr(repository_ctx.attr.licenses),
        "",
    ]

    if repository_ctx.attr.annotation_only:
        jar = repository_ctx.name + "-java"
        jar_name = jar + ".jar"

        repository_ctx.download(
            repository_ctx.attr.artifact_urls,
            output = jar_name,
            sha256 = repository_ctx.attr.artifact_sha256,
        )

        rule_lines = [
            "java_import(",
            "    name = '%s'," % jar,
            "    jars = ['%s']," % jar_name,
            ")",
            "j2cl_import(",
            "    name = '%s'," % repository_ctx.name,
            "    jar = ':%s'," % jar,
        ]

    else:
        repository_ctx.download_and_extract(
            repository_ctx.attr.artifact_urls,
            sha256 = repository_ctx.attr.artifact_sha256,
        )

        rule_lines = [
            "SUPER_SRCS = glob(['**/super/**/*.java'])",
            "REPLACED_SRCS = [s.split('/super/')[1] for s in SUPER_SRCS]",
            "",
            "j2cl_library(",
            "    name = '%s'," % repository_ctx.name,
            "    srcs = glob(['**/*.java', '**/*.js'],",
            "        exclude = REPLACED_SRCS + ['**/*_CustomFieldSerializer*']",
            "    ),",
            "    js_suppress = ['deprecated'],",
        ]

    lines.extend(rule_lines)

    for prop in _PASS_PROPS:
        value = getattr(repository_ctx.attr, prop, None)
        if value:
            if prop.endswith("_"):
                prop = prop[:-1]
            lines.append("    %s = %s," % (prop, repr(value)))

    lines.append(")")
    lines.append("")

    repository_ctx.file("BUILD", "\n".join(lines))

def _decode_maven_coordinates(artifact, default_packaging):
    parts = artifact.split(":")
    group_id = parts[0]
    artifact_id = parts[1]
    version = parts[2]
    classifier = None
    packaging = default_packaging
    if len(parts) == 4:
        packaging = parts[2]
        version = parts[3]
    elif len(parts) == 5:
        packaging = parts[2]
        classifier = parts[3]
        version = parts[4]

    return struct(
        group_id = group_id,
        artifact_id = artifact_id,
        version = version,
        classifier = classifier,
        packaging = packaging,
    )

# This method is public for usage in android.bzl macros
def convert_artifact_coordinate_to_urls(artifact, server_urls, packaging):
    """This function converts a Maven artifact coordinate into URLs."""
    coordinates = _decode_maven_coordinates(artifact, packaging)
    return _convert_coordinates_to_urls(coordinates, server_urls)

def _convert_coordinates_to_urls(coordinates, server_urls):
    group_id = coordinates.group_id.replace(".", "/")
    classifier = coordinates.classifier

    if classifier:
        classifier = "-" + classifier
    else:
        classifier = ""

    final_name = coordinates.artifact_id + "-" + coordinates.version + classifier + "." + coordinates.packaging
    url_suffix = group_id + "/" + coordinates.artifact_id + "/" + coordinates.version + "/" + final_name

    urls = []
    for server_url in server_urls:
        urls.append(_concat_with_needed_slash(server_url, url_suffix))
    return urls

def _concat_with_needed_slash(server_url, url_suffix):
    if server_url.endswith("/"):
        return server_url + url_suffix
    else:
        return server_url + "/" + url_suffix

j2cl_import_external = repository_rule(
    attrs = {
        "licenses": attr.string_list(default = ["none"]),
        "artifact_urls": attr.string_list(
            mandatory = True,
            allow_empty = False,
        ),
        "annotation_only": attr.bool(default = False),
        "artifact_sha256": attr.string(),
        "additional_rule_attrs": attr.string_dict(),
        "deps": attr.string_list(),
        "runtime_deps": attr.string_list(),
        "testonly_": attr.bool(),
        "exports": attr.string_list(),
        "default_visibility": attr.string_list(default = ["//visibility:public"]),
    },
    implementation = _j2cl_import_external,
)

def j2cl_maven_import_external(
        artifact,
        server_urls,
        annotation_only = False,
        **kwargs):
    coordinates = _decode_maven_coordinates(artifact, default_packaging = "jar")
    if annotation_only:
        src_coordinates = coordinates
    else:
        src_coordinates = struct(
            group_id = coordinates.group_id,
            artifact_id = coordinates.artifact_id,
            version = coordinates.version,
            classifier = "sources",
            packaging = "jar",
        )

    srcjar_urls = _convert_coordinates_to_urls(src_coordinates, server_urls)
    tags = kwargs.pop("tags", [])
    tags.append("maven_coordinates=" + artifact)

    j2cl_import_external(
        artifact_urls = srcjar_urls,
        tags = tags,
        annotation_only = annotation_only,
        **kwargs
    )

workspace(name = "com_google_j2cl")

maven_jar(
    name = "args4j",
    artifact = "args4j:args4j:2.33",
)

maven_jar(
  name = "com_google_auto_common",
  artifact = "com.google.auto:auto-common:0.9",
)

maven_jar(
  name = "com_google_auto_service",
  artifact = "com.google.auto.service:auto-service:1.0-rc2",
)

maven_jar(
    name = "com_google_auto_value",
    artifact = "com.google.auto.value:auto-value:1.4",
)

maven_jar(
    name = "com_google_guava",
    artifact = "com.google.guava:guava:21.0",
)

maven_jar(
    name = "com_google_jscomp",
    artifact = "com.google.javascript:closure-compiler:v20170409",
)

maven_jar(
    name = "com_google_jsinterop_annotations",
    artifact = "com.google.jsinterop:jsinterop-annotations:1.0.2",
)

maven_jar(
    name = "com_google_jsr305",
    artifact = "com.google.code.findbugs:jsr305:3.0.1",
)

maven_jar(
    name = "org_apache_commons_collections",
    artifact = "commons-collections:commons-collections:3.2.2",
)

maven_jar(
    name = "org_apache_commons_lang2",
    artifact = "commons-lang:commons-lang:2.6",
)

maven_jar(
    name = "org_apache_commons_lang3",
    artifact = "org.apache.commons:commons-lang3:3.5",
)

maven_jar(
  name = "org_apache_velocity",
  artifact = "org.apache.velocity:velocity:1.7",
)

http_jar(
    name = "org_eclipse_jdt_content_type",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar",
)

http_jar(
    name = "org_eclipse_jdt_jobs",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar",
)

http_jar(
    name = "org_eclipse_jdt_resources",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.resources_3.10.1.v20150725-1910.jar",
)

http_jar(
    name = "org_eclipse_jdt_runtime_compatibility",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.runtime.compatibility_3.2.300.v20150423-0821.jar",
)

http_jar(
    name = "org_eclipse_jdt_runtime",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar",
)

http_jar(
    name = "org_eclipse_jdt_equinox_common",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar",
)

http_jar(
    name = "org_eclipse_jdt_equinox_preferences",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar",
)

http_jar(
    name = "org_eclipse_jdt_compiler_apt",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.jdt.compiler.apt_1.2.0.v20150514-0146.jar",
)

http_jar(
    name = "org_eclipse_jdt_core",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar",
)

http_jar(
    name = "org_eclipse_jdt_osgi",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.osgi_3.10.102.v20160118-1700.jar",
)

http_jar(
    name = "org_eclipse_jdt_text",
    url = "http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/org.eclipse.text_3.5.400.v20150505-1044.jar",
)

new_http_archive(
  name="closure_library",
  url="https://github.com/google/closure-library/archive/v20170409.tar.gz",
  build_file="closure_library.BUILD",
  strip_prefix="closure-library-20170409"
)

http_archive(
  name="org_gwtproject_gwt",
  url="https://gwt.googlesource.com/gwt/+archive/master.tar.gz",
)

http_archive(
    name = "io_bazel_rules_closure",
    strip_prefix = "rules_closure-0.4.1",
    sha256 = "ba5e2e10cdc4027702f96e9bdc536c6595decafa94847d08ae28c6cb48225124",
    url = "http://mirror.bazel.build/github.com/bazelbuild/rules_closure/archive/0.4.1.tar.gz",
)

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_repositories")

closure_repositories(
    omit_args4j=True,
    omit_closure_library=True,
)


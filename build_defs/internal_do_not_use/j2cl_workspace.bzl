"""Macro to use for loading the J2CL repository"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_repositories")
load("@bazel_skylib//:lib/versions.bzl", "versions")

def setup_j2cl_workspace():
    """Load all dependencies needed for J2CL."""

    versions.check("0.23.0")  # The version J2CL currently have a CI setup for.

    closure_repositories(
        omit_com_google_protobuf = True,
        omit_com_google_auto_common = True,
    )

    native.maven_jar(
        name = "com_google_auto_common",
        artifact = "com.google.auto:auto-common:0.9",
    )

    native.maven_jar(
        name = "com_google_auto_service",
        artifact = "com.google.auto.service:auto-service:1.0-rc2",
    )

    # We cannot replace com_google_jsinterop_annotations so choose a different name
    native.maven_jar(
        name = "com_google_jsinterop_annotations_head",
        artifact = "com.google.jsinterop:jsinterop-annotations:HEAD-SNAPSHOT",
        repository = "https://oss.sonatype.org/content/repositories/google-snapshots/",
    )

    native.maven_jar(
        name = "org_apache_commons_collections",
        artifact = "commons-collections:commons-collections:3.2.2",
    )

    native.maven_jar(
        name = "org_apache_commons_lang2",
        artifact = "commons-lang:commons-lang:2.6",
    )

    native.maven_jar(
        name = "org_apache_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.6",
    )

    native.maven_jar(
        name = "org_apache_commons_text",
        artifact = "org.apache.commons:commons-text:1.2",
    )

    native.maven_jar(
        name = "org_apache_velocity",
        artifact = "org.apache.velocity:velocity:1.7",
    )

    native.maven_jar(
        name = "org_junit",
        artifact = "junit:junit:4.12",
    )

    native.maven_jar(
        name = "com_google_testing_compile",
        artifact = "com.google.testing.compile:compile-testing:0.15",
    )

    native.maven_jar(
        name = "org_mockito",
        artifact = "org.mockito:mockito-all:1.9.5",
    )

    native.maven_jar(
        name = "com_google_truth",
        artifact = "com.google.truth:truth:0.39",
    )

    # Eclipse JARs listed at
    # http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/

    http_jar(
        name = "org_eclipse_jdt_content_type",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.core.contenttype_3.7.300.v20190215-2048.jar",
        sha256 = "d4cee7a28a000a89863ab225c479cafc2fdf8638db2e89b383d6d792c5bfb866",
    )

    http_jar(
        name = "org_eclipse_jdt_jobs",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.core.jobs_3.10.300.v20190215-2048.jar",
        sha256 = "a337ff365732b08ce59be7e82921fe354ceccb58c6221fc2b064ded9449382b0",
    )

    http_jar(
        name = "org_eclipse_jdt_resources",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.core.resources_3.13.300.v20190218-2054.jar",
        sha256 = "a9176863b9be022cf841ec41e9e5080444700382b7b72ab5d0f3d33d540c1e10",
    )

    http_jar(
        name = "org_eclipse_jdt_runtime",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.core.runtime_3.15.200.v20190301-1641.jar",
        sha256 = "9a3b7af1bde36433768a21db8cba91e60c8b56de1cebcc1cbe35fc69c6cba4ef",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_common",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.equinox.common_3.10.300.v20190218-2100.jar",
        sha256 = "86252b5b8cf263f64438e78e80ed8f8b2b22664d4436d582aeb1b6ff9b3a806e",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_preferences",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.equinox.preferences_3.7.300.v20190218-2100.jar",
        sha256 = "d80f376963d276fbe88c9a117b343f39ea381538d104d100e46b5aeef089d1f3",
    )

    http_jar(
        name = "org_eclipse_jdt_compiler_apt",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.jdt.apt.core_3.6.300.v20190228-0624.jar",
        sha256 = "279f69572a11a969bf875a4d2bb27de279327bc843ea30fdc46a2ab3fcf46e8a",
    )

    http_jar(
        name = "org_eclipse_jdt_core",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.jdt.core_3.17.0.v20190306-2240.jar",
        sha256 = "2b23a72dd7dacfb90000c290fbd6d23fc3857cf015622397d7fc3bee9e7da259",
    )

    http_jar(
        name = "org_eclipse_jdt_osgi",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.osgi_3.13.300.v20190218-1622.jar",
        sha256 = "d1df0da964ec1bf0e5ac4f442eead3076c1c89dc834bc6148776ff5542c5a0e7",
    )

    http_jar(
        name = "org_eclipse_jdt_text",
        url = "http://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/org.eclipse.text_3.8.100.v20190306-1823.jar",
        sha256 = "e9972f7a70c3130691eb57d4514b22d33c6c7615acccab1107fb0e1d64fab710",
    )

    http_archive(
        name = "org_gwtproject_gwt",
        url = "https://gwt.googlesource.com/gwt/+archive/master.tar.gz",
    )

    # proto_library and java_proto_library rules implicitly depend on
    # @com_google_protobuf for protoc and proto runtimes.
    http_archive(
        name = "com_google_protobuf",
        strip_prefix = "protobuf-3.6.1.3",
        urls = ["https://github.com/google/protobuf/archive/v3.6.1.3.zip"],
    )

    # needed for protobuf
    native.bind(
        name = "guava",
        actual = "@com_google_guava",
    )

    # needed for protobuf
    native.bind(
        name = "gson",
        actual = "@com_google_code_gson",
    )

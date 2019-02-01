"""Macro to use for loading the J2CL repository"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")
load("@io_bazel_rules_closure//closure:defs.bzl", "closure_repositories")

def setup_j2cl_workspace():
    """Load all dependencies needed for J2CL."""

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
    # http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/

    http_jar(
        name = "org_eclipse_jdt_content_type",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.core.contenttype_3.7.200.v20181107-1343.jar",
        sha256 = "28b74f2a273a7a633845c315dbfe6b3bbc65e6fdefdb213fbecc43ded86fd8f2",
    )

    http_jar(
        name = "org_eclipse_jdt_jobs",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.core.jobs_3.10.200.v20180912-1356.jar",
        sha256 = "a5aaaaa2ffac532fa0582f32223cca91813e310d19fdf076ba230da1a2371533",
    )

    http_jar(
        name = "org_eclipse_jdt_resources",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.core.resources_3.13.200.v20181121-1020.jar",
        sha256 = "63c423ca7e8ae7aeb18eb91c60c632ccd11fd1da54f3d5e7601af83e730855d2",
    )

    http_jar(
        name = "org_eclipse_jdt_runtime",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.core.runtime_3.15.100.v20181107-1343.jar",
        sha256 = "3c089d14ffb9329dfdde75acbef481235abb1c98ad27bd7148aba48637c11e74",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_common",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.equinox.common_3.10.200.v20181021-1645.jar",
        sha256 = "224a35deeb64ea7271bce3d976974cd76e162e1366631ab01ada95426152fa24",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_preferences",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.equinox.preferences_3.7.200.v20180827-1235.jar",
        sha256 = "93c227ed2b6780d605ff48e93add77db00083f9b98a19392c6123b08caadbabd",
    )

    http_jar(
        name = "org_eclipse_jdt_compiler_apt",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.jdt.compiler.apt_1.3.400.v20181205-0900.jar",
        sha256 = "33541f28373d9e3277210c32ae9e4345851324a0c324a2c08411fe54b6028b9b",
    )

    http_jar(
        name = "org_eclipse_jdt_core",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.jdt.core_3.16.0.v20181130-1748.jar",
        sha256 = "7c71886a76964a825eb734d22dedbd3a1efa2c19bec3af26d07b7bbe8167d943",
    )

    http_jar(
        name = "org_eclipse_jdt_osgi",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.osgi_3.13.200.v20181130-2106.jar",
        sha256 = "03e5e8715d03605d0cf26ad93cbe005cffe070792bbc1aa27e0540aa0c1aa178",
    )

    http_jar(
        name = "org_eclipse_jdt_text",
        url = "http://download.eclipse.org/eclipse/updates/4.10/R-4.10-201812060815/plugins/org.eclipse.text_3.8.0.v20180923-1636.jar",
        sha256 = "bca08fbddb5b13a79be82b10c57105e2e6353b15a8b82b4a59d02b67618c92cf",
    )

    http_archive(
        name = "org_gwtproject_gwt",
        url = "https://gwt.googlesource.com/gwt/+archive/master.tar.gz",
    )

    # proto_library and java_proto_library rules implicitly depend on
    # @com_google_protobuf for protoc and proto runtimes.
    http_archive(
        name = "com_google_protobuf",
        strip_prefix = "protobuf-3.6.1",
        urls = ["https://github.com/google/protobuf/archive/v3.6.1.zip"],
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

"""Macro to use for loading the J2CL repository"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_jar")
load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies")
load("@bazel_skylib//lib:versions.bzl", "versions")
load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

_MAVEN_CENTRAL_URLS = ["https://repo1.maven.org/maven2/"]

def setup_j2cl_workspace():
    """Load all dependencies needed for J2CL."""

    versions.check("3.3.0")  # The version J2CL currently have a CI setup for.

    rules_closure_dependencies(
        omit_com_google_auto_common = True,
    )

    jvm_maven_import_external(
        name = "com_google_auto_common",
        artifact = "com.google.auto:auto-common:0.9",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_auto_service_annotations",
        artifact = "com.google.auto.service:auto-service-annotations:1.0-rc7",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_auto_service",
        artifact = "com.google.auto.service:auto-service:1.0-rc7",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    # We cannot replace com_google_jsinterop_annotations so choose a different name
    http_archive(
        name = "com_google_jsinterop_annotations-j2cl",
        urls = ["https://github.com/google/jsinterop-annotations/archive/2.0.1.zip"],
        strip_prefix = "jsinterop-annotations-2.0.1",
        sha256 = "29379768aa64811f8bcb515a9da2a4eb44acd6b439ea20b3e622155687e6843d",
    )

    jvm_maven_import_external(
        name = "org_apache_commons_collections",
        artifact = "commons-collections:commons-collections:3.2.2",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_apache_commons_lang2",
        artifact = "commons-lang:commons-lang:2.6",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_apache_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.6",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_apache_commons_text",
        artifact = "org.apache.commons:commons-text:1.2",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_apache_velocity",
        artifact = "org.apache.velocity:velocity:1.7",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_junit",
        artifact = "junit:junit:4.12",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_testing_compile",
        artifact = "com.google.testing.compile:compile-testing:0.15",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "org_mockito",
        artifact = "org.mockito:mockito-all:1.9.5",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    jvm_maven_import_external(
        name = "com_google_truth",
        artifact = "com.google.truth:truth:1.0",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    # TODO(b/135461024): for now J2CL uses a prepackaged version of javac. But in the future it
    # might be better to tie in to the Java platform in bazel and control the version there.
    jvm_maven_import_external(
        name = "com_sun_tools_javac",
        artifact = "com.google.errorprone:javac:9+181-r4173-1",
        server_urls = _MAVEN_CENTRAL_URLS,
        licenses = ["notice"],
    )

    # Eclipse JARs listed at
    # http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/

    http_jar(
        name = "org_eclipse_jdt_content_type",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.core.contenttype_3.7.300.v20190215-2048.jar",
        sha256 = "d4cee7a28a000a89863ab225c479cafc2fdf8638db2e89b383d6d792c5bfb866",
    )

    http_jar(
        name = "org_eclipse_jdt_jobs",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.core.jobs_3.10.400.v20190506-1457.jar",
        sha256 = "31dce5a10508774c84ff06d5840ce90b04eeb5c1a22314abe1afbe0f402007e8",
    )

    http_jar(
        name = "org_eclipse_jdt_resources",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.core.resources_3.13.400.v20190505-1655.jar",
        sha256 = "c7ea0920453e16dd72c43d7ee47b2d9cd45a5267c92984ad51e09cb6a8c4378d",
    )

    http_jar(
        name = "org_eclipse_jdt_runtime",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.core.runtime_3.15.300.v20190508-0543.jar",
        sha256 = "a9dc5b43defbb9975136c31478eef9f66ca567be8f4b1d47814752173e18f299",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_common",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.equinox.common_3.10.400.v20190516-1504.jar",
        sha256 = "85cde16fb71b9f25271916b11a5cdce50ddc79cb2974484085990d152e945661",
    )

    http_jar(
        name = "org_eclipse_jdt_equinox_preferences",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.equinox.preferences_3.7.400.v20190516-1504.jar",
        sha256 = "298c1ee84e68f0eb011be5d55e4041525b682d034ae5ebc7dafdfcb86ef09d6f",
    )

    http_jar(
        name = "org_eclipse_jdt_compiler_apt",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.jdt.apt.core_3.6.400.v20190328-1431.jar",
        sha256 = "589097f07dd37570117e078a56da8b754c22a4225fae09f1a09e5b6f81d58ba0",
    )

    http_jar(
        name = "org_eclipse_jdt_core",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.jdt.core_3.18.0.v20190522-0428.jar",
        sha256 = "2a727df878a41bf63f2f6bd0a77fb327ffebd1b637f4b06f08cb41263ff96194",
    )

    http_jar(
        name = "org_eclipse_jdt_osgi",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.osgi_3.14.0.v20190517-1309.jar",
        sha256 = "0a97384b1820f709695e76e9eca214b8bed59738206945113bb89c5fc5060e12",
    )

    http_jar(
        name = "org_eclipse_jdt_text",
        url = "http://download.eclipse.org/eclipse/updates/4.12/R-4.12-201906051800/plugins/org.eclipse.text_3.8.200.v20190519-2344.jar",
        sha256 = "3c0747c95459cfadf6d11ef591452c98812a9208d40a9fe3719ba7dbf8a26132",
    )

    http_archive(
        name = "org_gwtproject_gwt",
        strip_prefix = "gwt-release-2.9.0",
        url = "https://github.com/gwtproject/gwt/archive/release/2.9.0.zip",
        sha256 = "e4931f9d6ff4477e16e22cdfe318c69c0f2088c1528d8743ce54c530cd3e89fa",
    )

    http_archive(
        name = "org_jbox2d",
        strip_prefix = "jbox2d-jbox2d-2.2.1.1/jbox2d-library",
        urls = ["https://github.com/jbox2d/jbox2d/archive/jbox2d-2.2.1.1.zip"],
        sha256 = "088e5fc0f56c75f82c289c4721d9faf46a309e258d3ee647799622ef82e60303",
        patches = ["@com_google_j2cl//transpiler/javatests/com/google/j2cl/transpiler/integration/box2d:jbox2d.patch"],
        build_file_content = '''
filegroup(
    name = "j2cl_sources",
    srcs = glob(
        ["src/main/java/**/*.java"],
        exclude = [
            "**/StrictMath.java",
            "**/PlatformMathUtils.java",
            "**/Timer.java",
            "**/profile/**",
        ],
    ) + glob(["src/main/java/org/jbox2d/gwtemul/**/*.java"]),
    visibility = ["//visibility:public"],
)''',
    )

    # Required by protobuf_java_util
    native.bind(
        name = "guava",
        actual = "@com_google_guava",
    )

    # Required by protobuf_java_util
    native.bind(
        name = "gson",
        actual = "@com_google_code_gson",
    )

    # Required by protobuf_java_util
    native.bind(
        name = "error_prone_annotations",
        actual = "@com_google_errorprone_error_prone_annotations",
    )

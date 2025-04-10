"""J2WASM allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

J2WASM_ALLOWLIST = allowlists.of_packages([
    "//third_party/java/animal_sniffer/...",
    "//third_party/java/auto/...",
    "//third_party/java/checker_framework_annotations/...",
    "//third_party/java/dagger/...",
    "//third_party/java/error_prone/...",
    "//third_party/java/jakarta_inject/...",
    "//third_party/java/jsr250_annotations/...",
    "//third_party/java/jsr330_inject/...",
    "//third_party/java/junit/...",
    "//third_party/java/qo_metafile/...",
    "//third_party/java/qo_ole/...",
    "//third_party/java/re2j/...",
    "//third_party/java_src/animal_sniffer/...",
    "//third_party/java_src/dagger/...",
    "//third_party/java_src/google_common/current/...",
    "//third_party/java_src/jakarta_inject/...",
    "//...",
    "//build_defs/internal_do_not_use/...",
    "//junit/...",
    "//jre/...",
    "//samples/...",
    "//third_party/...",
    "//transpiler/...",
    "//third_party/java_src/jsr330_inject/...",
    "//third_party/java_src/qo_metafile/...",
    "//third_party/java_src/qo_ole/...",
    "//third_party/java_src/re2j/...",
])

load(":generate_wasm_array.bzl", "generate_wasm_array", "generate_wasm_array_subtype")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//jre/java:__pkg__"],
    licenses = ["notice"],
)

# To view the generated file, use:
# blaze build jre/java/super-wasm/javaemul/internal/wasmarray:WasmArray.java
# gnome-open blaze-genfiles/jre/java/super-wasm/javaemul/internal/wasmarray/

filegroup(
    name = "wasmarray",
    srcs = [
        ":src_WasmArray",
    ],
)

generate_wasm_array(
    name = "WasmArray",
    subtypes = [
        "OfObject",
        "OfBoolean",
        "OfByte",
        "OfShort",
        "OfInt",
        "OfLong",
        "OfChar",
        "OfFloat",
        "OfDouble",
    ],
)

generate_wasm_array_subtype(
    name = "OfObject",
    elementDefaultValue = "null",
    elementTypeName = "java.lang.Object",
)

generate_wasm_array_subtype(
    name = "OfBoolean",
    elementDefaultValue = "false",
    elementTypeName = "boolean",
)

generate_wasm_array_subtype(
    name = "OfByte",
    elementDefaultValue = "0",
    elementTypeName = "byte",
)

generate_wasm_array_subtype(
    name = "OfShort",
    elementDefaultValue = "0",
    elementTypeName = "short",
)

generate_wasm_array_subtype(
    name = "OfInt",
    elementDefaultValue = "0",
    elementTypeName = "int",
)

generate_wasm_array_subtype(
    name = "OfLong",
    elementDefaultValue = "0L",
    elementTypeName = "long",
)

generate_wasm_array_subtype(
    name = "OfChar",
    elementDefaultValue = "'\\0'",
    elementTypeName = "char",
)

generate_wasm_array_subtype(
    name = "OfFloat",
    elementDefaultValue = "0f",
    elementTypeName = "float",
)

generate_wasm_array_subtype(
    name = "OfDouble",
    elementDefaultValue = "0.0",
    elementTypeName = "double",
)

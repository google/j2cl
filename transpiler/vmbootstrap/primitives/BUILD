# Description:
#   j2cl/vmbootstrap/primitives: Primitive helper classes.
#
# Depends on the JRE JS.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(":generate_primitive.bzl", "generate_primitive_type")

# You can see the generated file with following commands:
# blaze build third_party/java_src/j2cl/transpiler/vmbootstrap/primitives:all
# gnome-open blaze-genfiles/third_party/java_src/j2cl/transpiler/vmbootstrap/primitives/

generate_primitive_type(name = "double")

generate_primitive_type(name = "float")

generate_primitive_type(name = "int")

generate_primitive_type(name = "short")

generate_primitive_type(name = "byte")

generate_primitive_type(name = "char")

generate_primitive_type(name = "void")

generate_primitive_type(
    name = "long",
    initValue = "Long.fromInt(0)",
    jsTypeName = "Long",
)

generate_primitive_type(
    name = "boolean",
    initValue = "false",
    jsTypeName = "boolean",
)

filegroup(
    name = "primitives",
    srcs = glob(["*.js"]) + [
        ":src_double",
        ":src_float",
        ":src_long",
        ":src_int",
        ":src_short",
        ":src_byte",
        ":src_char",
        ":src_boolean",
        ":src_void",
    ],
)

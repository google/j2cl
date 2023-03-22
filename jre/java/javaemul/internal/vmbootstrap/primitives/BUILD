# Description:
#   j2cl/vmbootstrap/primitives: Primitive helper classes.
#
# Depends on the JRE JS.

load(":generate_primitive.bzl", "generate_primitive_type")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//jre/java:__pkg__"],
    licenses = ["notice"],
)

# You can see the generated file with following commands:
# blaze build //jre/java/javaemul/internal/vmbootstrap/primitives:all
# gnome-open blaze-genfiles/jre/java/javaemul/internal/vmbootstrap/primitives/

generate_primitive_type(
    name = "double",
    boxedType = "java.lang.Double",
    shortName = "D",
)

generate_primitive_type(
    name = "float",
    boxedType = "java.lang.Float",
    shortName = "F",
)

generate_primitive_type(
    name = "int",
    boxedType = "java.lang.Integer",
    shortName = "I",
)

generate_primitive_type(
    name = "short",
    boxedType = "java.lang.Short",
    shortName = "S",
)

generate_primitive_type(
    name = "byte",
    boxedType = "java.lang.Byte",
    shortName = "B",
)

generate_primitive_type(
    name = "char",
    boxedType = "java.lang.Character",
    shortName = "C",
)

generate_primitive_type(
    name = "void",
    boxedType = "java.lang.Void",
    shortName = "_",
)

generate_primitive_type(
    name = "long",
    boxedType = "java.lang.Long",
    initValue = "Long.fromInt(0)",
    jsTypeName = "Long",
    shortName = "J",
)

generate_primitive_type(
    name = "boolean",
    boxedType = "java.lang.Boolean",
    initValue = "false",
    jsTypeName = "boolean",
    shortName = "Z",
)

filegroup(
    name = "primitives",
    srcs = glob(["*.js"]) + [
        ":src_boolean",
        ":src_byte",
        ":src_char",
        ":src_double",
        ":src_float",
        ":src_int",
        ":src_long",
        ":src_short",
        ":src_void",
    ],
)

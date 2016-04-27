package com.google.j2cl.transpiler.readable.annotationsincomments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

// TODO(simionato): Delete this file once we decide on an annotation to express nullability.
@Target(ElementType.PACKAGE)
public @interface PackageDefaultNonNullable {}

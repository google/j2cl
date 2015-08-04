package com.google.j2cl.transpiler.readable.subclassgenericclass;

class GenericClass<T> {}

class GenericSubclassGenericClass<T> extends GenericClass<T> {}

public class SubclassGenericClass extends GenericClass<SubclassGenericClass> {}

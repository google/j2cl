package com.google.j2cl.transpiler.readable.implementsgenericinterface;

interface GenericInterface<T> {}

interface GenericSubInterface<T> extends GenericInterface<T> {}

class GenericInterfaceGenericImpl<T> implements GenericInterface<T> {}

public class GenericInterfaceImpl implements GenericInterface<Number> {}

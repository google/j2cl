package com.google.j2cl.transpiler.readable.declarelegacynamespace;

import jsinterop.annotations.JsType;

@JsType
public abstract class DeclareLegacyNamespaceJsType {

  public static final DeclareLegacyNamespaceJsType TRUTHY = new DeclareLegacyNamespaceJsType() {
    @Override
    public boolean foo() {
      return true;
    }
  };

  public abstract boolean foo();
}

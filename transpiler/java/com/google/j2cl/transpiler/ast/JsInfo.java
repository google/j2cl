/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.ast;

import com.google.auto.value.AutoValue;
import com.google.j2cl.common.ThreadLocalInterner;
import javax.annotation.Nullable;

/**
 * Encapsulates JsInterop information.
 */
@AutoValue
public abstract class JsInfo {
  public abstract JsMemberType getJsMemberType();

  /**
   * The name specified directly on a type, method or field.
   */
  @Nullable
  public abstract String getJsName();

  /**
   * The namespace specified on a package, type, method or field.
   */
  @Nullable
  public abstract String getJsNamespace();

  public abstract boolean isJsOverlay();

  public abstract boolean isJsAsync();

  /**
   * Returns true if the JsInfo where computed from a JsInterop annotation defined explicitly on the
   * member.
   */
  // TODO(b/266614719): Remove when the annotation data is part of our model.
  public abstract boolean getHasJsMemberAnnotation();

  abstract Builder toBuilder();

  /** Not a JS member. */
  public static final JsInfo NONE = newBuilder().setJsMemberType(JsMemberType.NONE).build();

  public static final JsInfo NONE_ASYNC =
      newBuilder().setJsMemberType(JsMemberType.NONE).setJsAsync(true).build();

  public static final JsInfo RAW = newBuilder().setJsMemberType(JsMemberType.METHOD).build();
  // Used to prevent name mangling in a couple of special methods that are considered overlays
  // (e.g. clints in native types)
  public static final JsInfo RAW_OVERLAY =
      newBuilder().setJsMemberType(JsMemberType.METHOD).setJsOverlay(true).build();
  public static final JsInfo RAW_CTOR =
      newBuilder().setJsMemberType(JsMemberType.CONSTRUCTOR).build();
  public static final JsInfo RAW_FIELD =
      newBuilder().setJsMemberType(JsMemberType.PROPERTY).build();

  public static Builder newBuilder() {
    return new AutoValue_JsInfo.Builder()
        // Default values.
        .setJsOverlay(false)
        .setJsAsync(false)
        .setHasJsMemberAnnotation(false);
  }

  /** A Builder for JsInfo. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setJsMemberType(JsMemberType jsMemberType);

    public abstract Builder setJsName(String jsName);

    public abstract Builder setJsNamespace(String jsNamespace);

    public abstract Builder setJsOverlay(boolean isJsOverlay);

    public abstract Builder setJsAsync(boolean isJsAsync);

    public abstract Builder setHasJsMemberAnnotation(boolean hasJsMemberAnnotation);

    abstract JsInfo autoBuild();

    public JsInfo build() {
      return interner.intern(autoBuild());
    }

    public static Builder from(JsInfo jsInfo) {
      return jsInfo.toBuilder();
    }

    private static final ThreadLocalInterner<JsInfo> interner = new ThreadLocalInterner<>();
  }
}

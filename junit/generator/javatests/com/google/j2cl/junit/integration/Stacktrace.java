/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.junit.integration;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** Represents a stacktrace. */
@AutoValue
public abstract class Stacktrace {

  private static final String OPTIONAL_STACKFRAME = "__OPTIONAL__";

  public static boolean isOptionalFrame(String frame) {
    return OPTIONAL_STACKFRAME.equals(frame);
  }

  public abstract ImmutableList<String> frames();

  public abstract String message();

  /** Builder for stack trace. */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract ImmutableList.Builder<String> framesBuilder();

    public Builder addOptionalFrame() {
      framesBuilder().add(OPTIONAL_STACKFRAME);
      return this;
    }

    public Builder addFrame(String frame) {
      framesBuilder().add(frame);
      return this;
    }

    public abstract Builder message(String message);

    public abstract Stacktrace build();
  }

  public static Builder newStacktraceBuilder() {
    return new AutoValue_Stacktrace.Builder();
  }

  public static Stacktrace parse(String stacktrace) {
    Builder builder = newStacktraceBuilder();
    String message = "";
    for (String frame : stacktrace.split("\\n")) {
      // cut off comments
      int index = frame.indexOf('#');
      if (index != -1) {
        frame = frame.substring(0, index);
      }
      // if the frame is empty after removing the comment do not add it
      if (frame.trim().isEmpty()) {
        continue;
      }

      if (frame.startsWith(" ")) {
        builder.addFrame(frame.trim());
      } else {
        message = message.isEmpty() ? frame : (message + "\n" + frame);
      }
    }

    return builder.message(message).build();
  }

}

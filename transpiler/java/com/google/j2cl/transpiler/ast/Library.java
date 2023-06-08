/*
 * Copyright 2021 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.List;
import java.util.stream.Stream;

/** Class representing a library, which is the collection of compilation units compiled together. */
@Visitable
public class Library extends Node {
  @Visitable List<CompilationUnit> compilationUnits;

  private Library(List<CompilationUnit> compilationUnits) {
    this.compilationUnits = checkNotNull(compilationUnits);
  }

  public List<CompilationUnit> getCompilationUnits() {
    return compilationUnits;
  }

  public Stream<Type> streamTypes() {
    return compilationUnits.stream().flatMap(c -> c.streamTypes());
  }

  public boolean isEmpty() {
    return compilationUnits.isEmpty();
  }

  @Override
  public Library clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Library.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for Library. */
  public static class Builder {
    private List<CompilationUnit> compilationUnits;

    public static Builder from(Library library) {
      return newBuilder().setCompilationUnits(library.getCompilationUnits());
    }

    public Builder setCompilationUnits(List<CompilationUnit> compilationUnits) {
      this.compilationUnits = compilationUnits;
      return this;
    }

    public Library build() {
      return new Library(compilationUnits);
    }
  }
}

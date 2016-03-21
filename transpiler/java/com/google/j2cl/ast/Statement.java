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
package com.google.j2cl.ast;

import com.google.j2cl.ast.processors.HasMetadata;
import com.google.j2cl.ast.processors.Visitable;
import com.google.j2cl.ast.sourcemap.SourceInfo;
import com.google.j2cl.ast.sourcemap.TracksSourceInfo;

/**
 * A base class for Statement.
 */
@Visitable
public abstract class Statement extends Node implements TracksSourceInfo {
  // unknown by default.
  private SourceInfo javaSourceInfo = SourceInfo.UNKNOWN_SOURCE_INFO;

  private SourceInfo javaScriptSourceInfo = SourceInfo.UNKNOWN_SOURCE_INFO;

  @Override
  public SourceInfo getJavaSourceInfo() {
    return javaSourceInfo;
  }

  @Override
  public void setJavaSourceInfo(SourceInfo sourceInfo) {
    javaSourceInfo = sourceInfo;
  }

  public SourceInfo getOutputSourceInfo() {
    return javaScriptSourceInfo;
  }

  public void setOutputSourceInfo(SourceInfo sourceInfo) {
    this.javaScriptSourceInfo = sourceInfo;
  }

  @Override
  public void copyMetadataFrom(HasMetadata<TracksSourceInfo> store) {
    setJavaSourceInfo(store.getMetadata().getJavaSourceInfo());
  }

  @Override
  public TracksSourceInfo getMetadata() {
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Statement.visit(processor, this);
  }
}

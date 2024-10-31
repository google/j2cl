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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/** Method declaration. */
@Visitable
public class Method extends Member implements MethodLike {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;
  private final String jsDocDescription;
  private final String wasmExportName;
  @Nullable private Boolean isForcedJavaOverride;

  private Method(
      SourcePosition sourcePosition,
      MethodDescriptor methodDescriptor,
      List<Variable> parameters,
      Block body,
      String jsDocDescription,
      String wasmExportName,
      @Nullable Boolean isForcedJavaOverride) {
    super(sourcePosition);
    this.methodDescriptor = checkNotNull(methodDescriptor);
    this.parameters.addAll(checkNotNull(parameters));
    this.jsDocDescription = jsDocDescription;
    this.wasmExportName = wasmExportName;
    this.body = checkNotNull(body);
    this.isForcedJavaOverride = isForcedJavaOverride;
  }

  @Override
  public MethodDescriptor getDescriptor() {
    return methodDescriptor;
  }

  @Override
  public List<Variable> getParameters() {
    return parameters;
  }

  @Override
  public Block getBody() {
    return body;
  }

  public void setBody(Block body) {
    this.body = checkNotNull(body);
  }

  @Override
  public boolean isConstructor() {
    return methodDescriptor.isConstructor();
  }

  @Override
  public boolean isMethod() {
    return true;
  }

  public boolean isBridge() {
    return methodDescriptor.isGeneralizingdBridge();
  }

  @Override
  public boolean isAbstract() {
    return methodDescriptor.isAbstract();
  }

  public String getJsDocDescription() {
    return jsDocDescription;
  }

  public String getWasmInfo() {
    return methodDescriptor.getWasmInfo();
  }

  public boolean isWasmEntryPoint() {
    return wasmExportName != null;
  }

  /** The name of the export for the Wasm entry point. */
  @Nullable
  public String getWasmExportName() {
    return wasmExportName;
  }

  @Nullable
  public Boolean isForcedJavaOverride() {
    return isForcedJavaOverride;
  }

  public void setForcedJavaOverride(@Nullable Boolean isForcedJavaOverride) {
    this.isForcedJavaOverride = isForcedJavaOverride;
  }

  public final boolean isJavaOverride() {
    return isForcedJavaOverride != null ? isForcedJavaOverride : methodDescriptor.isJavaOverride();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String getReadableDescription() {
    // TODO(b/138398080): Add name to the parameter abstraction in MethodDescriptor and just
    //  delegate to ParameterDescriptor for the description.
    String parameterString =
        getParameters().stream().map(this::getParameterReadableDescription).collect(joining(", "));

    TypeDeclaration enclosingTypeDeclaration =
        getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration();
    if (isConstructor()) {
      return String.format(
          "%s(%s)", enclosingTypeDeclaration.getReadableDescription(), parameterString);
    }
    return String.format(
        "%s %s.%s(%s)",
        getDescriptor().getReturnTypeDescriptor().getReadableDescription(),
        enclosingTypeDeclaration.getReadableDescription(),
        getDescriptor().getName(),
        parameterString);
  }

  private String getParameterReadableDescription(Variable parameter) {
    if (parameter == getVarargsParameter()) {
      ArrayTypeDescriptor parameterTypeDescriptor =
          (ArrayTypeDescriptor) parameter.getTypeDescriptor();
      return String.format(
          "%s... %s",
          parameterTypeDescriptor.getComponentTypeDescriptor().getReadableDescription(),
          parameter.getName());
    }
    return String.format(
        "%s %s", parameter.getTypeDescriptor().getReadableDescription(), parameter.getName());
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Method.visit(processor, this);
  }

  /** Returns true if the method is locally empty (allows calls to super constructor). */
  public boolean isEmpty() {
    // TODO(rluble): to be completely correct the parameters of the implicit supercall need to
    // be examined.
    List<Statement> statements = getBody().getStatements();
    return statements.isEmpty()
        || (statements.size() == 1 && isConstructor() && AstUtils.hasSuperCall(this));
  }

  /**
   * A Builder for Method.
   *
   * <p>Takes care of the busy work of keeping parameter list and method descriptor parameter type
   * list in sync.
   */
  public static class Builder {

    private MethodDescriptor methodDescriptor;
    private List<Variable> parameters = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private String jsDocDescription;
    private String wasmExportName;
    private SourcePosition bodySourcePosition;
    private SourcePosition sourcePosition;
    @Nullable private Boolean isForcedJavaOverride;

    public static Builder from(Method method) {
      Builder builder = new Builder();
      builder.methodDescriptor = method.getDescriptor();
      builder.parameters = Lists.newArrayList(method.getParameters());
      builder.statements = Lists.newArrayList(method.getBody().getStatements());
      builder.jsDocDescription = method.getJsDocDescription();
      builder.wasmExportName = method.getWasmExportName();
      builder.bodySourcePosition = method.getBody().getSourcePosition();
      builder.sourcePosition = method.getSourcePosition();
      builder.isForcedJavaOverride = method.isForcedJavaOverride();
      return builder;
    }

    @CanIgnoreReturnValue
    public Builder addParameters(int index, Variable... parameters) {
      return addParameters(index, Arrays.asList(parameters));
    }

    @CanIgnoreReturnValue
    public Builder addParameters(int index, Collection<Variable> newParameters) {
      parameters.addAll(index, newParameters);
      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .addParameterTypeDescriptors(
                  index,
                  newParameters.stream()
                      .map(Variable::getTypeDescriptor)
                      .collect(toImmutableList()))
              .build();
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setStatements(List<Statement> statements) {
      this.statements = new ArrayList<>(statements);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    @CanIgnoreReturnValue
    public Builder addStatements(Statement... statements) {
      Collections.addAll(this.statements, statements);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addStatements(List<Statement> statements) {
      this.statements.addAll(statements);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addStatement(int index, Statement statement) {
      this.statements.add(index, statement);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setParameters(Variable... parameters) {
      return setParameters(Arrays.asList(parameters));
    }

    @CanIgnoreReturnValue
    public Builder setParameters(Collection<Variable> parameters) {
      this.parameters = new ArrayList<>(parameters);
      checkState(parameters.size() == methodDescriptor.getParameterDescriptors().size());
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setJsDocDescription(String jsDocDescription) {
      this.jsDocDescription = jsDocDescription;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setWasmExportName(String wasmExportName) {
      this.wasmExportName = wasmExportName;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setBodySourcePosition(SourcePosition sourcePosition) {
      this.bodySourcePosition = sourcePosition;
      return this;
    }

    public Method build() {
      Block body =
          Block.newBuilder()
              .setSourcePosition(bodySourcePosition != null ? bodySourcePosition : sourcePosition)
              .setStatements(statements)
              .build();
      checkState(parameters.size() == methodDescriptor.getParameterDescriptors().size());
      checkState(methodDescriptor.isDeclaration());

      return new Method(
          sourcePosition,
          methodDescriptor,
          parameters,
          body,
          jsDocDescription,
          wasmExportName,
          isForcedJavaOverride);
    }
  }
}

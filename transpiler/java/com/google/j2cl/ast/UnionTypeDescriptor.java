package com.google.j2cl.ast;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Visitable;

import java.util.List;

/**
 * A (by name) reference to an union type, which is used in catch clause.
 */
@AutoValue
@Visitable
public abstract class UnionTypeDescriptor extends TypeDescriptor {
  public static UnionTypeDescriptor create(List<TypeDescriptor> types) {
    return new AutoValue_UnionTypeDescriptor(ImmutableList.copyOf(types));
  }

  public abstract ImmutableList<TypeDescriptor> getTypes();

  @Override
  public boolean isRaw() {
    return false;
  }

  @Override
  public int getDimensions() {
    return 0;
  }

  @Override
  public TypeDescriptor getLeafTypeDescriptor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSimpleName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getBinaryName() {
    return Joiner.on(" | ")
        .join(
            Lists.transform(
                getTypes(),
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor type) {
                    return type.getBinaryName();
                  }
                }));
  }

  @Override
  public String getClassName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getCompilationUnitSourceName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSourceName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPackageName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_UnionTypeDescriptor.visit(processor, this);
  }

}

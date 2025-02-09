/*
 * Copyright 2025 Google Inc.
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

public class RecordField extends Member {

    private final Field field;

    public RecordField(Field field) {
        super(field.getSourcePosition());
        this.field = field;
    }

    @Override
    public MemberDescriptor getDescriptor() {
        return field.fieldDescriptor;
    }

    public Field getField() {
        return field;
    }
}

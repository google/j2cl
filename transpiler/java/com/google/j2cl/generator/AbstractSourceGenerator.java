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
package com.google.j2cl.generator;

import com.google.j2cl.errors.Errors;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * A base class for source generators. We may have two subclasses, which are
 * JavaScriptGenerator and SourceMapGenerator.
 */
public abstract class AbstractSourceGenerator {
  protected final Errors errors;
  protected final boolean declareLegacyNamespace;

  public AbstractSourceGenerator(Errors errors, boolean declareLegacyNamespace) {
    this.errors = errors;
    this.declareLegacyNamespace = declareLegacyNamespace;
  }

  public void writeToFile(Path outputPath, Charset charset) {
    GeneratorUtils.writeToFile(outputPath, toSource(), charset, errors);
  }

  public abstract String toSource();

  public abstract String getSuffix();
}

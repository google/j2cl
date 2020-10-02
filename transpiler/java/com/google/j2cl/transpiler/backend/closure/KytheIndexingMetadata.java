/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.transpiler.backend.closure;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Representation of Kythe indexing metadata */
@SuppressWarnings("unused") // Fields are accessed through reflection by GSON.
public class KytheIndexingMetadata {

  /** Object that describes a file. */
  private static class VName {
    private final String corpus;

    private final String path;

    private final String root;

    private VName(String corpus, String path, String root) {
      this.corpus = corpus;
      this.path = path;
      this.root = root;
    }
  }

  /**
   * Mapping of an entity from the original .java file to the generated .js file. Entities are
   * things like class, method, field and similar.
   */
  private static class AnchorAnchorMetadata {
    private final String type = "anchor_anchor";

    /** Byte offset of the start position of the identifier in .java file. */
    @SerializedName("source_begin")
    private final int sourceBegin;

    /** Byte offset of the end position of the identifier in .java file. */
    @SerializedName("source_end")
    private final int sourceEnd;

    /** Byte offset of the start position of the identifier in .js file. */
    @SerializedName("target_begin")
    private final int targetBegin;

    /** Byte offset of the end position of the identifier in .js file. */
    @SerializedName("target_end")
    private final int targetEnd;

    private final String edge = "/kythe/edge/imputes";

    /** Info about source .java file that contains current entity. */
    @SerializedName("source_vname")
    private final VName sourceVName;

    private AnchorAnchorMetadata(
        int sourceBegin,
        int sourceEnd,
        int targetBegin,
        int targetEnd,
        String corpus,
        String path,
        String root) {
      this.sourceBegin = sourceBegin;
      this.sourceEnd = sourceEnd;
      this.targetBegin = targetBegin;
      this.targetEnd = targetEnd;
      this.sourceVName = new VName(corpus, path, root);
    }
  }

  private final String type = "kythe0";

  private final List<AnchorAnchorMetadata> meta = new ArrayList<>();

  public void addAnchorAnchor(
      int sourcebegin,
      int sourceend,
      int targetbegin,
      int targetend,
      String sourceCorpus,
      String sourcePath,
      String sourceRoot) {
    if (sourceCorpus == null) {
      sourceCorpus = "google3";
    }
    meta.add(
        new AnchorAnchorMetadata(
            sourcebegin, sourceend, targetbegin, targetend, sourceCorpus, sourcePath, sourceRoot));
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}

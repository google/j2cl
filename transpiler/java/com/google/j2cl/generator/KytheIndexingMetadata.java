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
package com.google.j2cl.generator;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Representation of Kythe indexing metadata */
@SuppressWarnings("unused") // Fields are accessed through reflection by GSON.
public class KytheIndexingMetadata {
  static class VName {
    private final String corpus;

    private final String path;

    private final String root;

    public VName(String corpus, String path, String root) {
      this.corpus = corpus;
      this.path = path;
      this.root = root;
    }
  }

  static class AnchorAnchorMetadata {
    private final String type = "anchor_anchor";

    @SerializedName("source_begin")
    private final int sourceBegin;

    @SerializedName("source_end")
    private final int sourceEnd;

    @SerializedName("target_begin")
    private final int targetBegin;

    @SerializedName("target_end")
    private final int targetEnd;

    private final String edge = "/kythe/edge/imputes";

    @SerializedName("source_vname")
    private final VName sourceVName;

    public AnchorAnchorMetadata(
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

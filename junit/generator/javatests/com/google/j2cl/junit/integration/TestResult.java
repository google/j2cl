/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.junit.integration;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

@AutoValue
public abstract class TestResult {

  abstract ImmutableList<String> succeeds();

  abstract ImmutableMultimap<String, String> fails();

  abstract ImmutableMap<String, String> errors();

  abstract ImmutableList<ImmutableList<String>> javaLogLinesSequences();

  abstract ImmutableList<String> blackList();

  abstract String testClassName();

  abstract String packageName();

  abstract boolean failedToInstantiateTest();

  abstract ImmutableList<String> logLinesSequences();

  public static Builder builder() {
    return new AutoValue_TestResult.Builder().failedToInstantiateTest(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    abstract ImmutableList.Builder<String> succeedsBuilder();

    abstract ImmutableMultimap.Builder<String, String> failsBuilder();

    abstract ImmutableMap.Builder<String, String> errorsBuilder();

    abstract ImmutableList.Builder<ImmutableList<String>> javaLogLinesSequencesBuilder();

    abstract ImmutableList.Builder<String> logLinesSequencesBuilder();

    abstract ImmutableList.Builder<String> blackListBuilder();

    public abstract Builder testClassName(String testClassName);

    public abstract Builder packageName(String packageName);

    public abstract Builder failedToInstantiateTest(boolean failedToInstantiateTest);

    public Builder addTestSuccess(String testName) {
      succeedsBuilder().add(testName);
      return this;
    }

    public Builder addTestFailure(String testName) {
      return addTestFailure(testName, "");
    }

    public Builder addTestFailure(String testName, String errorMessage) {
      failsBuilder().put(testName, errorMessage);
      return this;
    }

    public Builder addTestError(String testName, String errorMessage) {
      errorsBuilder().put(testName, errorMessage);
      return this;
    }

    public Builder addJavaLogLineSequence(String... sequence) {
      javaLogLinesSequencesBuilder().add(ImmutableList.copyOf(sequence));
      return this;
    }

    public Builder addLogLineContains(String logLine) {
      logLinesSequencesBuilder().add(logLine);
      return this;
    }

    public Builder addBlackListedWord(String blackListed) {
      blackListBuilder().add(blackListed);
      return this;
    }

    public abstract TestResult build();
  }
}

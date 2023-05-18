/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.common.bazel;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ForkJoinPool;

/**
 * A cache that caches outputs produced from files while minimizing locking.
 *
 * <p>The cache makes sure that if the underlying file is updated, the output will be refreshed as
 * well.
 */
public final class FileCache<T> {

  /** Interface for function to obtain the information to cache for a file. */
  public interface FileFunction<V> {
    V apply(Path path) throws IOException;
  }

  private final LoadingCache<String, CachedFile> cache;
  private final FileFunction<T> fn;

  public FileCache(FileFunction<T> fn, int cacheSize) {
    this(fn, cacheSize, ForkJoinPool.getCommonPoolParallelism());
  }

  public FileCache(FileFunction<T> fn, int cacheSize, int parallelism) {
    this.fn = fn;
    this.cache =
        CacheBuilder.newBuilder()
            .maximumSize(cacheSize)
            .concurrencyLevel(parallelism)
            .build(CacheLoader.from(CachedFile::new));
  }

  public T get(String path) {
    return cache.getUnchecked(path).get();
  }

  private class CachedFile {
    private final Path path;
    private volatile FileTime lastModified;
    private volatile T cached;

    private CachedFile(String fileName) {
      this.path = Paths.get(fileName);
    }

    private T get() {
      // Note that the method is not synchronized and it is ok to calculate cached object twice in
      // case of unlike event of a race condition.
      try {
        FileTime newLastModified = Files.getLastModifiedTime(path);
        if (!newLastModified.equals(lastModified)) {
          cached = fn.apply(path);
          lastModified = newLastModified;
        }
        return cached;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

/*
 * Copyright 2020 Google Inc.
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
package java.io;

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.nio.charset.Charset;
import javaemul.internal.EmulatedCharset;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/io/OutputStreamWriter.html">the
 * official Java API doc</a> for details.
 */
public class OutputStreamWriter extends Writer {

  private final OutputStream out;

  private final Charset charset;

  private final char[] surrogateBuffer = new char[2];

  public OutputStreamWriter(OutputStream out, String charsetName) {
    this(out, Charset.forName(charsetName));
  }

  public OutputStreamWriter(OutputStream out, Charset charset) {
    this.out = checkNotNull(out);
    this.charset = checkNotNull(charset);
  }

  @Override
  public void close() throws IOException {
    if (surrogateBuffer[0] != 0) {
      out.write('?');
    }
    out.close();
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  public String getEncoding() {
    return charset.name();
  }

  @Override
  public void write(char[] buffer, int offset, int count) throws IOException {
    IOUtils.checkOffsetAndCount(buffer, offset, count);

    if (count == 0) {
      return;
    }

    // If we have a pending high surrogate, insert it in front of a copy of the buffer
    if (surrogateBuffer[0] != 0) {
      surrogateBuffer[1] = buffer[offset++];
      count--;
      out.write(((EmulatedCharset) charset).getBytes(surrogateBuffer, 0, 2));
      surrogateBuffer[0] = 0;
    }

    // If our data ends in a high surrogate, the low surrogate is missing and we need to remove
    // it from the current conversion.
    if (count > 0 && Character.isHighSurrogate(buffer[count - 1])) {
      surrogateBuffer[0] = buffer[--count];
    }
    byte[] bytes = ((EmulatedCharset) charset).getBytes(buffer, offset, count);
    out.write(bytes, 0, bytes.length);
  }
}

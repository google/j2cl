/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.io;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html">the official
 * Java API doc</a> for details.
 */
public class PrintWriter extends Writer {

  protected Writer out;

  /** Indicates whether this PrintWriter is in an error state. */
  private boolean ioError;

  /**
   * Indicates whether or not this PrintWriter should flush its contents after printing a new line.
   */
  private final boolean autoFlush;

  public PrintWriter(OutputStream out) {
    this(new OutputStreamWriter(out), false);
  }

  public PrintWriter(OutputStream out, boolean autoFlush) {
    this(new OutputStreamWriter(out), autoFlush);
  }

  public PrintWriter(Writer wr) {
    this(wr, false);
  }

  public PrintWriter(Writer wr, boolean autoFlush) {
    this.autoFlush = autoFlush;
    out = wr;
  }

  public boolean checkError() {
    Writer delegate = out;
    if (delegate == null) {
      return ioError;
    }

    flush();
    if (delegate instanceof PrintWriter) {
      return ((PrintWriter) delegate).checkError();
    }
    return ioError;
  }

  protected void clearError() {
    ioError = false;
  }

  @Override
  public void close() {
    if (out != null) {
      try {
        out.close();
      } catch (IOException e) {
        setError();
      }
      out = null;
    }
  }

  @Override
  public void flush() {
    if (out != null) {
      try {
        out.flush();
      } catch (IOException e) {
        setError();
      }
    } else {
      setError();
    }
  }

  public void print(char[] charArray) {
    print(new String(charArray, 0, charArray.length));
  }

  public void print(char ch) {
    print(String.valueOf(ch));
  }

  public void print(double dnum) {
    print(String.valueOf(dnum));
  }

  public void print(float fnum) {
    print(String.valueOf(fnum));
  }

  public void print(int inum) {
    print(String.valueOf(inum));
  }

  public void print(long lnum) {
    print(String.valueOf(lnum));
  }

  public void print(Object obj) {
    print(String.valueOf(obj));
  }

  public void print(String str) {
    write(str != null ? str : String.valueOf((Object) null));
  }

  public void print(boolean bool) {
    print(String.valueOf(bool));
  }

  public void println() {
    print('\n');
    if (autoFlush) {
      flush();
    }
  }

  public void println(char[] chars) {
    println(new String(chars, 0, chars.length));
  }

  public void println(char c) {
    println(String.valueOf(c));
  }

  public void println(double d) {
    println(String.valueOf(d));
  }

  public void println(float f) {
    println(String.valueOf(f));
  }

  public void println(int i) {
    println(String.valueOf(i));
  }

  public void println(long l) {
    println(String.valueOf(l));
  }

  public void println(Object obj) {
    println(String.valueOf(obj));
  }

  public void println(String str) {
    print(str);
    println();
  }

  public void println(boolean b) {
    println(String.valueOf(b));
  }

  protected void setError() {
    ioError = true;
  }

  @Override
  public void write(char[] buf) {
    write(buf, 0, buf.length);
  }

  @Override
  public void write(char[] buf, int offset, int count) {
    doWrite(buf, offset, count);
  }

  @Override
  public void write(int oneChar) {
    doWrite(new char[] {(char) oneChar}, 0, 1);
  }

  private final void doWrite(char[] buf, int offset, int count) {
    if (out != null) {
      try {
        out.write(buf, offset, count);
      } catch (IOException e) {
        setError();
      }
    } else {
      setError();
    }
  }

  @Override
  public void write(String str) {
    write(str.toCharArray());
  }

  @Override
  public void write(String str, int offset, int count) {
    write(str.substring(offset, offset + count).toCharArray());
  }

  @Override
  public PrintWriter append(char c) {
    write(c);
    return this;
  }

  @Override
  public PrintWriter append(CharSequence csq) {
    if (csq == null) {
      csq = "null";
    }
    append(csq, 0, csq.length());
    return this;
  }

  @Override
  public PrintWriter append(CharSequence csq, int start, int end) {
    if (csq == null) {
      csq = "null";
    }
    String output = csq.subSequence(start, end).toString();
    write(output, 0, output.length());
    return this;
  }
}

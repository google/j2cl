/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.minifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * A thread-safe, fast and pretty minifier/comment stripper for J2CL generated code.
 *
 * <p>Converts mangled J2CL names to minified (but still pretty and unique) versions and strips
 * block comments (doing both these things while doing no AST construction and respecting the
 * sanctity of strings).
 *
 * <p>The imagined use case for this minifier is to be part of fast concatenating uncompiled JS
 * servers used as part of the development cycle. There is an expectation that because uncompiled
 * and non-@JsType J2CL output is mangled that it might be large enough to slow down the browser as
 * well as be difficult to directly read and this minifier addresses that problem. Callers should
 * reuse the same minifier instance across multiple files to get consistent minification.
 *
 * <p>It is expected that minification opportunities will only be found in J2CL generated files
 * (files ending in .java.js) since only such files should contain references to mangled J2CL names.
 * So if some caller wants to optimize their minifier usage they might consider invoking it only on
 * .java.js files.
 *
 * <p>Line comments are recognized and preserved, mostly to keep the sourcemap comment but also to
 * make sure that the parser doesn't interpret the contents of a line comment and let it effect the
 * parse state.
 *
 * <p>SourceMap updating would be nice. This may be added later.
 */
public class J2clMinifier {

  private interface TransitionFunction {
    StringBuilder transition(
        StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c);
  }

  /**
   * This is a unicode fullwidth low line. Using this unusual separator and incrementing number
   * makes it extremely unlikely that a new minified name will collide with some existing
   * identifier.
   */
  private static final String MINIFICATION_SEPARATOR = "\uFF3F";

  private static final int[][] nextState;
  private static final int S_BLOCK_COMMENT;
  private static final int S_BLOCK_COMMENT_END;
  private static final int S_DOUBLE_QUOTED_STRING;
  private static final int S_DOUBLE_QUOTED_STRING_ESCAPE;
  private static final int S_NON_MINIMIZABLE_IDENTIFIER;
  private static final int S_MINIMIZABLE_IDENTIFIER;
  private static final int S_LINE_COMMENT;
  private static final int S_LINE_COMMENT_END;
  private static final int S_MAYBE_BLOCK_COMMENT_END;
  private static final int S_MAYBE_COMMENT_START;
  private static final int S_NON_IDENTIFIER;
  private static final int S_SINGLE_QUOTED_STRING;
  private static final int S_SINGLE_QUOTED_STRING_ESCAPE;
  private static final int S_STRING_END;

  private static int stateIndex = 0;

  static {
    // Initialize unique state ids.
    {
      S_BLOCK_COMMENT = stateIndex++;
      S_BLOCK_COMMENT_END = stateIndex++;
      S_DOUBLE_QUOTED_STRING = stateIndex++;
      S_DOUBLE_QUOTED_STRING_ESCAPE = stateIndex++;
      S_NON_MINIMIZABLE_IDENTIFIER = stateIndex++;
      S_MINIMIZABLE_IDENTIFIER = stateIndex++;
      S_LINE_COMMENT = stateIndex++;
      S_LINE_COMMENT_END = stateIndex++;
      S_MAYBE_BLOCK_COMMENT_END = stateIndex++;
      S_MAYBE_COMMENT_START = stateIndex++;
      S_NON_IDENTIFIER = stateIndex++;
      S_SINGLE_QUOTED_STRING = stateIndex++;
      S_SINGLE_QUOTED_STRING_ESCAPE = stateIndex++;
      S_STRING_END = stateIndex++;
    }

    // Create and initialize state transitions table.
    {
      nextState = new int[stateIndex][256];

      setDefaultTransitions(S_NON_IDENTIFIER, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_NON_IDENTIFIER);
      setCommentOrStringStartTransitions(S_NON_IDENTIFIER);

      setDefaultTransitions(S_MINIMIZABLE_IDENTIFIER, S_NON_IDENTIFIER);
      setIdentifierCharTransitions(S_MINIMIZABLE_IDENTIFIER, S_MINIMIZABLE_IDENTIFIER);
      setCommentOrStringStartTransitions(S_MINIMIZABLE_IDENTIFIER);

      setDefaultTransitions(S_NON_MINIMIZABLE_IDENTIFIER, S_NON_IDENTIFIER);
      setIdentifierCharTransitions(S_NON_MINIMIZABLE_IDENTIFIER, S_NON_MINIMIZABLE_IDENTIFIER);
      setCommentOrStringStartTransitions(S_NON_MINIMIZABLE_IDENTIFIER);

      setDefaultTransitions(S_MAYBE_COMMENT_START, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_MAYBE_COMMENT_START);
      nextState[S_MAYBE_COMMENT_START]['/'] = S_LINE_COMMENT;
      nextState[S_MAYBE_COMMENT_START]['*'] = S_BLOCK_COMMENT;
      nextState[S_MAYBE_COMMENT_START]['\''] = S_SINGLE_QUOTED_STRING;
      nextState[S_MAYBE_COMMENT_START]['"'] = S_DOUBLE_QUOTED_STRING;

      setDefaultTransitions(S_LINE_COMMENT, S_LINE_COMMENT);
      nextState[S_LINE_COMMENT]['\n'] = S_LINE_COMMENT_END;

      setDefaultTransitions(S_LINE_COMMENT_END, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_LINE_COMMENT_END);
      setCommentOrStringStartTransitions(S_LINE_COMMENT_END);

      setDefaultTransitions(S_BLOCK_COMMENT, S_BLOCK_COMMENT);
      nextState[S_BLOCK_COMMENT]['*'] = S_MAYBE_BLOCK_COMMENT_END;

      setDefaultTransitions(S_MAYBE_BLOCK_COMMENT_END, S_BLOCK_COMMENT);
      nextState[S_MAYBE_BLOCK_COMMENT_END]['/'] = S_BLOCK_COMMENT_END;
      nextState[S_MAYBE_BLOCK_COMMENT_END]['*'] = S_MAYBE_BLOCK_COMMENT_END;

      setDefaultTransitions(S_BLOCK_COMMENT_END, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_BLOCK_COMMENT_END);
      setCommentOrStringStartTransitions(S_BLOCK_COMMENT_END);

      setDefaultTransitions(S_SINGLE_QUOTED_STRING, S_SINGLE_QUOTED_STRING);
      nextState[S_SINGLE_QUOTED_STRING]['\\'] = S_SINGLE_QUOTED_STRING_ESCAPE;
      nextState[S_SINGLE_QUOTED_STRING]['\''] = S_STRING_END;

      setDefaultTransitions(S_DOUBLE_QUOTED_STRING, S_DOUBLE_QUOTED_STRING);
      nextState[S_DOUBLE_QUOTED_STRING]['\\'] = S_DOUBLE_QUOTED_STRING_ESCAPE;
      nextState[S_DOUBLE_QUOTED_STRING]['"'] = S_STRING_END;

      setDefaultTransitions(S_SINGLE_QUOTED_STRING_ESCAPE, S_SINGLE_QUOTED_STRING);

      setDefaultTransitions(S_DOUBLE_QUOTED_STRING_ESCAPE, S_DOUBLE_QUOTED_STRING);

      setDefaultTransitions(S_STRING_END, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_STRING_END);
      setCommentOrStringStartTransitions(S_STRING_END);
    }
  }

  public static boolean isJ2clFile(String filePath) {
    return filePath.endsWith(".java.js");
  }

  private static StringBuilder bufferIdentifierChar(
      @SuppressWarnings("unused") StringBuilder minifiedContentBuffer,
      StringBuilder identifierBuffer,
      char c) {
    identifierBuffer.append(c);
    return identifierBuffer;
  }

  private static String computePrettyIdentifier(String identifier) {
    // Because we have a different mangling pattern for meta functions you can't extract the pretty
    // name with a single simple regex match group.

    if (startsLikeJavaMethodOrField(identifier)) {
      // It's a regular field or method, extract its name.
      int beginIndex = identifier.indexOf('_') + 1;
      int endIndex = identifier.indexOf("__", beginIndex);
      return identifier.substring(beginIndex, endIndex);
    } else {
      // It's one of the meta functions like "$create__".
      return identifier.substring(1, identifier.indexOf('_'));
    }
  }

  private static char[] getChars(String content) {
    char[] chars = new char[content.length()];
    content.getChars(0, content.length(), chars, 0);
    return chars;
  }

  private static boolean isIdentifierChar(char c) {
    return c == '_'
        || c == '$'
        || (c >= '0' && c <= '9')
        || (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z');
  }

  private static boolean isMinifiableIdentifier(String identifier) {
    // This is faster than a regex and more readable as well.
    if (startsLikeJavaMethodOrField(identifier)) {
      int underScoreIndex = identifier.indexOf('_');
      // Match mangled Java member names of the form:  m_<name>__<par1>_ ....
      return identifier.indexOf("__", underScoreIndex + 1) != -1;
    }

    return identifier.startsWith("$create__")
        || identifier.startsWith("$ctor__")
        || identifier.startsWith("$implements__")
        || identifier.startsWith("$init__");
  }

  private static boolean startsLikeJavaMethodOrField(String identifier) {
    return identifier.startsWith("f_")
        || identifier.startsWith("m_")
        || identifier.startsWith("$f_");
  }

  private static void setDefaultTransitions(int currentState, int nextState) {
    for (char c = 0; c < 256; c++) {
      J2clMinifier.nextState[currentState][c] = nextState;
    }
  }

  private static void setIdentifierStartTransitions(int currentState) {
    setIdentifierCharTransitions(currentState, S_NON_MINIMIZABLE_IDENTIFIER);
    nextState[currentState]['f'] = S_MINIMIZABLE_IDENTIFIER;
    nextState[currentState]['m'] = S_MINIMIZABLE_IDENTIFIER;
    nextState[currentState]['$'] = S_MINIMIZABLE_IDENTIFIER;
  }

  private static void setIdentifierCharTransitions(int currentState, int nextState) {
    for (char c = 0; c < 256; c++) {
      if (isIdentifierChar(c)) {
        J2clMinifier.nextState[currentState][c] = nextState;
      }
    }
  }

  private static void setCommentOrStringStartTransitions(int currentState) {
    nextState[currentState]['/'] = S_MAYBE_COMMENT_START;
    nextState[currentState]['\''] = S_SINGLE_QUOTED_STRING;
    nextState[currentState]['"'] = S_DOUBLE_QUOTED_STRING;
  }

  @SuppressWarnings("unused")
  private static StringBuilder skipChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    return identifierBuffer;
  }

  private static StringBuilder skipCharUnlessNewLine(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    if (c == '\n') {
      identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, c);
    }
    return identifierBuffer;
  }

  @SuppressWarnings("unused")
  private static StringBuilder startNewIdentifier(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    StringBuilder identifierBuilder = new StringBuilder();
    identifierBuilder.append(c);
    return identifierBuilder;
  }

  private static StringBuilder writeChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    minifiedContentBuffer.append(c);
    return identifierBuffer;
  }

  private static StringBuilder writeSlashAndChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/');
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, c);
    return identifierBuffer;
  }

  private static StringBuilder writeSlashAndStartNewIdentifier(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/');
    identifierBuffer = startNewIdentifier(minifiedContentBuffer, identifierBuffer, c);
    return identifierBuffer;
  }

  /**
   * These fields contain the persistent state that allows for name collision dodging and consistent
   * renaming within and across multiple files.
   */
  private final Multiset<String> countsByIdentifier = HashMultiset.<String>create();

  /**
   * This is a cache of previously minified content (presumably whole files). This makes reloads in
   * fast concatenating uncompiled JS servers extra-extra fast.
   */
  private final Map<String, String> minifiedContentByContent = new Hashtable<>();

  private final TransitionFunction[][] transFn;

  @VisibleForTesting Map<String, String> minifiedIdentifiersByIdentifier = new HashMap<>();

  public J2clMinifier() {
    // This will be much shorter once we can use Java 8 here.
    TransitionFunction startNewIdentifier =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return startNewIdentifier(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction writeChar =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return writeChar(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction skipChar =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return skipChar(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction bufferIdentifierChar =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return bufferIdentifierChar(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction writeIdentifierAndChar =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return writeIdentifierAndChar(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction writeIdentifier =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return writeIdentifier(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction writeSlashAndStartNewIdentifier =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return writeSlashAndStartNewIdentifier(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction writeSlashAndChar =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return writeSlashAndChar(minifiedContentBuffer, identifierBuffer, c);
          }
        };
    TransitionFunction skipCharUnlessNewLine =
        new TransitionFunction() {
          @Override
          public StringBuilder transition(
              StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
            return skipCharUnlessNewLine(minifiedContentBuffer, identifierBuffer, c);
          }
        };

    transFn = new TransitionFunction[stateIndex][stateIndex];

    // Transitions out of these four states are handled very similarly, but we're choosing not
    // to logically combine the four states since there's no single name that accurately captures
    // all four ideas.
    {
      transFn[S_NON_IDENTIFIER][S_MINIMIZABLE_IDENTIFIER] = startNewIdentifier;
      transFn[S_NON_IDENTIFIER][S_NON_MINIMIZABLE_IDENTIFIER] = writeChar;
      transFn[S_NON_IDENTIFIER][S_NON_IDENTIFIER] = writeChar;
      transFn[S_NON_IDENTIFIER][S_MAYBE_COMMENT_START] = skipChar;
      transFn[S_NON_IDENTIFIER][S_SINGLE_QUOTED_STRING] = writeChar;
      transFn[S_NON_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = writeChar;

      transFn[S_LINE_COMMENT_END][S_MINIMIZABLE_IDENTIFIER] = startNewIdentifier;
      transFn[S_LINE_COMMENT_END][S_NON_MINIMIZABLE_IDENTIFIER] = writeChar;
      transFn[S_LINE_COMMENT_END][S_NON_IDENTIFIER] = writeChar;
      transFn[S_LINE_COMMENT_END][S_MAYBE_COMMENT_START] = skipChar;
      transFn[S_LINE_COMMENT_END][S_SINGLE_QUOTED_STRING] = writeChar;
      transFn[S_LINE_COMMENT_END][S_DOUBLE_QUOTED_STRING] = writeChar;

      transFn[S_BLOCK_COMMENT_END][S_MINIMIZABLE_IDENTIFIER] = startNewIdentifier;
      transFn[S_BLOCK_COMMENT_END][S_NON_MINIMIZABLE_IDENTIFIER] = writeChar;
      transFn[S_BLOCK_COMMENT_END][S_NON_IDENTIFIER] = writeChar;
      transFn[S_BLOCK_COMMENT_END][S_MAYBE_COMMENT_START] = skipChar;
      transFn[S_BLOCK_COMMENT_END][S_SINGLE_QUOTED_STRING] = writeChar;
      transFn[S_BLOCK_COMMENT_END][S_DOUBLE_QUOTED_STRING] = writeChar;

      transFn[S_STRING_END][S_MINIMIZABLE_IDENTIFIER] = startNewIdentifier;
      transFn[S_STRING_END][S_NON_MINIMIZABLE_IDENTIFIER] = writeChar;
      transFn[S_STRING_END][S_NON_IDENTIFIER] = writeChar;
      transFn[S_STRING_END][S_MAYBE_COMMENT_START] = skipChar;
      transFn[S_STRING_END][S_SINGLE_QUOTED_STRING] = writeChar;
      transFn[S_STRING_END][S_DOUBLE_QUOTED_STRING] = writeChar;
    }

    transFn[S_NON_MINIMIZABLE_IDENTIFIER][S_NON_MINIMIZABLE_IDENTIFIER] = writeChar;
    transFn[S_NON_MINIMIZABLE_IDENTIFIER][S_NON_IDENTIFIER] = writeChar;
    transFn[S_NON_MINIMIZABLE_IDENTIFIER][S_MAYBE_COMMENT_START] = skipChar;
    transFn[S_NON_MINIMIZABLE_IDENTIFIER][S_SINGLE_QUOTED_STRING] = writeChar;
    transFn[S_NON_MINIMIZABLE_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = writeChar;

    transFn[S_MINIMIZABLE_IDENTIFIER][S_MINIMIZABLE_IDENTIFIER] = bufferIdentifierChar;
    transFn[S_MINIMIZABLE_IDENTIFIER][S_NON_IDENTIFIER] = writeIdentifierAndChar;
    transFn[S_MINIMIZABLE_IDENTIFIER][S_MAYBE_COMMENT_START] = writeIdentifier;
    transFn[S_MINIMIZABLE_IDENTIFIER][S_SINGLE_QUOTED_STRING] = writeIdentifierAndChar;
    transFn[S_MINIMIZABLE_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = writeIdentifierAndChar;

    transFn[S_MAYBE_COMMENT_START][S_MINIMIZABLE_IDENTIFIER] = writeSlashAndStartNewIdentifier;
    transFn[S_MAYBE_COMMENT_START][S_NON_MINIMIZABLE_IDENTIFIER] = writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_MAYBE_COMMENT_START] = writeChar;
    transFn[S_MAYBE_COMMENT_START][S_LINE_COMMENT] = writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_BLOCK_COMMENT] = skipChar;
    transFn[S_MAYBE_COMMENT_START][S_NON_IDENTIFIER] = writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_SINGLE_QUOTED_STRING] = writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_DOUBLE_QUOTED_STRING] = writeSlashAndChar;

    transFn[S_LINE_COMMENT][S_LINE_COMMENT] = writeChar;
    transFn[S_LINE_COMMENT][S_LINE_COMMENT_END] = writeChar;

    transFn[S_BLOCK_COMMENT][S_BLOCK_COMMENT] = skipCharUnlessNewLine;
    transFn[S_BLOCK_COMMENT][S_MAYBE_BLOCK_COMMENT_END] = skipCharUnlessNewLine;

    transFn[S_MAYBE_BLOCK_COMMENT_END][S_BLOCK_COMMENT] = skipCharUnlessNewLine;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_BLOCK_COMMENT_END] = skipChar;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_MAYBE_BLOCK_COMMENT_END] = skipChar;

    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING] = writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING_ESCAPE] = writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_STRING_END] = writeChar;

    transFn[S_DOUBLE_QUOTED_STRING][S_DOUBLE_QUOTED_STRING] = writeChar;
    transFn[S_DOUBLE_QUOTED_STRING][S_DOUBLE_QUOTED_STRING_ESCAPE] = writeChar;
    transFn[S_DOUBLE_QUOTED_STRING][S_STRING_END] = writeChar;

    transFn[S_SINGLE_QUOTED_STRING_ESCAPE][S_SINGLE_QUOTED_STRING] = writeChar;

    transFn[S_DOUBLE_QUOTED_STRING_ESCAPE][S_DOUBLE_QUOTED_STRING] = writeChar;
  }

  public String minify(String content) {
    // Return a previously cached version of minified output, if possible.
    String minifiedContent = minifiedContentByContent.get(content);
    if (minifiedContent != null) {
      return minifiedContent;
    }

    char[] chars = getChars(content);
    StringBuilder minifiedContentBuffer = new StringBuilder();
    StringBuilder identifierBuffer = new StringBuilder();
    int lastParseState = S_NON_IDENTIFIER;

    /**
     * Loop over the chars in the content, keeping track of in/not-in identifier state, copying
     * non-identifier chars immediately and accumulating identifiers chars for minifying and copying
     * when the identifier ends.
     */
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      int parseState = nextState[lastParseState][c < 256 ? c : 0];

      TransitionFunction transitionFunction = transFn[lastParseState][parseState];
      identifierBuffer = transitionFunction.transition(minifiedContentBuffer, identifierBuffer, c);

      lastParseState = parseState;
    }

    if (lastParseState == S_MINIMIZABLE_IDENTIFIER) {
      // If the content ended in an identifier then end the identifier.
      writeIdentifier(minifiedContentBuffer, identifierBuffer, ' ');
    } else if (lastParseState == S_MAYBE_COMMENT_START) {
      // If the content in what initially looked like it might have been the start of a block
      // comment, you now know it wasn't.
      identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/');
    }

    minifiedContent = minifiedContentBuffer.toString();
    // Update the minified content cache for next time.
    minifiedContentByContent.put(content, minifiedContent);

    return minifiedContent;
  }

  /**
   * The minifier might be used from multiple threads so make sure that this function (which along
   * with the makeUnique function, which is also only called from here, is the only place that
   * mutates class state) is synchronized.
   */
  private synchronized String getMinifiedIdentifier(String identifier) {
    if (minifiedIdentifiersByIdentifier.containsKey(identifier)) {
      return minifiedIdentifiersByIdentifier.get(identifier);
    }

    String prettyIdentifier = computePrettyIdentifier(identifier);
    if (prettyIdentifier.isEmpty()) {
      // The identifier must contain something strange like triple _'s. Leave the whole thing alone
      // just to be safe.
      minifiedIdentifiersByIdentifier.put(identifier, identifier);
      return identifier;
    }

    String uniquePrettyIdentifier = makeUnique(prettyIdentifier);
    minifiedIdentifiersByIdentifier.put(identifier, uniquePrettyIdentifier);

    return uniquePrettyIdentifier;
  }

  private String makeUnique(String identifier) {
    countsByIdentifier.add(identifier);
    return identifier + MINIFICATION_SEPARATOR + countsByIdentifier.count(identifier);
  }

  private StringBuilder writeIdentifier(
      StringBuilder minifiedContentBuffer,
      StringBuilder identifierBuffer,
      @SuppressWarnings("unused") char c) {
    String identifier = identifierBuffer.toString();
    if (isMinifiableIdentifier(identifier)) {
      minifiedContentBuffer.append(getMinifiedIdentifier(identifier));
    } else {
      minifiedContentBuffer.append(identifier);
    }
    return identifierBuffer;
  }

  private StringBuilder writeIdentifierAndChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c) {
    writeIdentifier(minifiedContentBuffer, identifierBuffer, c);
    minifiedContentBuffer.append(c);
    return identifierBuffer;
  }

  /**
   * Entry point to the minfier standalone binary.
   *
   * <p>Usage: minifier file-to-minifiy.js
   *
   * <p>Outputs results to stdout.
   */
  public static void main(String... args) throws IOException {
    Preconditions.checkState(args.length == 1, "Provide a input file to minify");
    String file = args[0];
    String contents = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
    System.out.println(new J2clMinifier().minify(contents));
  }
}

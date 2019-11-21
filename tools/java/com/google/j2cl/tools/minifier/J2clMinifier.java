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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.j2cl.tools.rta.CodeRemovalInfo;
import com.google.j2cl.tools.rta.LineRange;
import com.google.j2cl.tools.rta.UnusedLines;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state);
  }

  /**
   * This is a unicode fullwidth low line. Using this unusual separator and incrementing number
   * makes it extremely unlikely that a new minified name will collide with some existing
   * identifier.
   */
  private static final String MINIFICATION_SEPARATOR = "\uFF3F";

  private static final String ZIP_FILE_SEPARATOR = "!/";
  private static final int ZIP_FILE_SEPARATOR_OFFSET = ZIP_FILE_SEPARATOR.length();

  private static final int[][] nextState;
  private static final int S_BLOCK_COMMENT;
  private static final int S_DOUBLE_QUOTED_STRING;
  private static final int S_DOUBLE_QUOTED_STRING_ESCAPE;
  private static final int S_IDENTIFIER;
  private static final int S_LINE_COMMENT;
  private static final int S_MAYBE_BLOCK_COMMENT_END;
  private static final int S_MAYBE_COMMENT_START;
  private static final int S_NON_IDENTIFIER;
  private static final int S_SINGLE_QUOTED_STRING;
  private static final int S_SINGLE_QUOTED_STRING_ESCAPE;
  private static final int S_END_STATE;

  private static int numberOfStates = 0;

  static {
    // Initialize unique state ids.
    {
      S_BLOCK_COMMENT = numberOfStates++;
      S_DOUBLE_QUOTED_STRING = numberOfStates++;
      S_DOUBLE_QUOTED_STRING_ESCAPE = numberOfStates++;
      S_IDENTIFIER = numberOfStates++;
      S_LINE_COMMENT = numberOfStates++;
      S_MAYBE_BLOCK_COMMENT_END = numberOfStates++;
      S_MAYBE_COMMENT_START = numberOfStates++;
      S_NON_IDENTIFIER = numberOfStates++;
      S_SINGLE_QUOTED_STRING = numberOfStates++;
      S_SINGLE_QUOTED_STRING_ESCAPE = numberOfStates++;
      S_END_STATE = numberOfStates++;
    }

    // Create and initialize state transitions table.
    {
      nextState = new int[numberOfStates][256];

      setDefaultTransitions(S_NON_IDENTIFIER, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_NON_IDENTIFIER);
      setCommentOrStringStartTransitions(S_NON_IDENTIFIER);

      setDefaultTransitions(S_IDENTIFIER, S_NON_IDENTIFIER);
      setIdentifierCharTransitions(S_IDENTIFIER, S_IDENTIFIER);
      setCommentOrStringStartTransitions(S_IDENTIFIER);

      setDefaultTransitions(S_MAYBE_COMMENT_START, S_NON_IDENTIFIER);
      setIdentifierStartTransitions(S_MAYBE_COMMENT_START);
      nextState[S_MAYBE_COMMENT_START]['/'] = S_LINE_COMMENT;
      nextState[S_MAYBE_COMMENT_START]['*'] = S_BLOCK_COMMENT;
      nextState[S_MAYBE_COMMENT_START]['\''] = S_SINGLE_QUOTED_STRING;
      nextState[S_MAYBE_COMMENT_START]['"'] = S_DOUBLE_QUOTED_STRING;

      setDefaultTransitions(S_LINE_COMMENT, S_LINE_COMMENT);
      nextState[S_LINE_COMMENT]['\n'] = S_NON_IDENTIFIER;

      setDefaultTransitions(S_BLOCK_COMMENT, S_BLOCK_COMMENT);
      nextState[S_BLOCK_COMMENT]['*'] = S_MAYBE_BLOCK_COMMENT_END;

      setDefaultTransitions(S_MAYBE_BLOCK_COMMENT_END, S_BLOCK_COMMENT);
      nextState[S_MAYBE_BLOCK_COMMENT_END]['/'] = S_NON_IDENTIFIER;
      nextState[S_MAYBE_BLOCK_COMMENT_END]['*'] = S_MAYBE_BLOCK_COMMENT_END;

      setDefaultTransitions(S_SINGLE_QUOTED_STRING, S_SINGLE_QUOTED_STRING);
      nextState[S_SINGLE_QUOTED_STRING]['\\'] = S_SINGLE_QUOTED_STRING_ESCAPE;
      nextState[S_SINGLE_QUOTED_STRING]['\''] = S_NON_IDENTIFIER;

      setDefaultTransitions(S_DOUBLE_QUOTED_STRING, S_DOUBLE_QUOTED_STRING);
      nextState[S_DOUBLE_QUOTED_STRING]['\\'] = S_DOUBLE_QUOTED_STRING_ESCAPE;
      nextState[S_DOUBLE_QUOTED_STRING]['"'] = S_NON_IDENTIFIER;

      setDefaultTransitions(S_SINGLE_QUOTED_STRING_ESCAPE, S_SINGLE_QUOTED_STRING);

      setDefaultTransitions(S_DOUBLE_QUOTED_STRING_ESCAPE, S_DOUBLE_QUOTED_STRING);
    }
  }

  public static boolean isJ2clFile(String filePath) {
    return filePath.endsWith(".java.js");
  }

  private static StringBuilder bufferIdentifierChar(
      @SuppressWarnings("unused") StringBuilder minifiedContentBuffer,
      StringBuilder identifierBuffer,
      char c,
      int state) {
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
    char firstChar = identifier.charAt(0);
    if (firstChar != '$' && firstChar != 'm' && firstChar != 'f') {
      return false;
    }

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
    setIdentifierCharTransitions(currentState, S_IDENTIFIER);
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
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    return identifierBuffer;
  }

  private static StringBuilder skipCharUnlessNewLine(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    if (c == '\n') {
      identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, c, state);
    }
    return identifierBuffer;
  }

  @SuppressWarnings("unused")
  private static StringBuilder startNewIdentifier(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    StringBuilder identifierBuilder = new StringBuilder();
    identifierBuilder.append(c);
    return identifierBuilder;
  }

  private static StringBuilder writeCharOrReplace(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    if ((c == '\n' || c == 0) && state == S_NON_IDENTIFIER) {
      maybeReplaceGoogStatement(minifiedContentBuffer);
    }
    if (c != 0) {
      minifiedContentBuffer.append(c);
    }
    return identifierBuffer;
  }

  private static final String MODULE_NAME = "['\"][\\w\\.$]+['\"]";
  private static final Pattern GOOG_FORWARD_DECLARE =
      Pattern.compile("((?:let|var) [\\w$]+) = goog.forwardDeclare\\(" + MODULE_NAME + "\\);");
  private static final Pattern GOOG_REQUIRE =
      Pattern.compile("goog.require\\(" + MODULE_NAME + "\\);");

  private static void maybeReplaceGoogStatement(StringBuilder minifiedContentBuffer) {
    int start = minifiedContentBuffer.lastIndexOf("\n") + 1;
    int end = minifiedContentBuffer.length();
    if (start == end) {
      return;
    }

    // goog.forwardDeclare is only useful for compiler except the variable declaration.
    Matcher m = GOOG_FORWARD_DECLARE.matcher(minifiedContentBuffer).region(start, end);
    if (m.matches()) {
      minifiedContentBuffer.replace(start, minifiedContentBuffer.length(), m.group(1)).append(';');
      return;
    }

    // Unassigned goog.require is only useful for compiler and bundling.
    m = GOOG_REQUIRE.matcher(minifiedContentBuffer).region(start, end);
    if (m.matches()) {
      minifiedContentBuffer.delete(start, minifiedContentBuffer.length());
      return;
    }
  }

  private static StringBuilder writeChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    minifiedContentBuffer.append(c);
    return identifierBuffer;
  }

  @SuppressWarnings("unused")
  private static StringBuilder writeSlash(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/', state);
    return identifierBuffer;
  }

  private static StringBuilder writeSlashAndChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/', state);
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, c, state);
    return identifierBuffer;
  }

  private static StringBuilder writeSlashAndStartNewIdentifier(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    identifierBuffer = writeChar(minifiedContentBuffer, identifierBuffer, '/', state);
    identifierBuffer = startNewIdentifier(minifiedContentBuffer, identifierBuffer, c, state);
    return identifierBuffer;
  }

  private static String extractFileKey(String fullPath) {
    if (fullPath == null) {
      return null;
    }

    // Because the mapping is done during transpilation and j2cl doesn't know the final path of
    // the zip file, the key used is the path of the file inside the zip file.
    String key = fullPath;
    int keyStartIndex = fullPath.indexOf(ZIP_FILE_SEPARATOR);
    if (keyStartIndex > 0) {
      key = key.substring(keyStartIndex + ZIP_FILE_SEPARATOR_OFFSET);
    }

    return key;
  }

  /**
   * These fields contain the persistent state that allows for name collision dodging and consistent
   * renaming within and across multiple files.
   */
  private final Multiset<String> countsByIdentifier = HashMultiset.create();

  private final boolean minifierDisabled = Boolean.getBoolean("j2cl_minifier_disabled");

  /** Set of file paths that are not used by the application. */
  private ImmutableSet<String> unusedFiles;

  /**
   * Gives per file key the array of line indexes that can be stripped. If the index of the line
   * being scanned is out of bounds of the array, it means the line is used. Otherwise the boolean
   * stored at the index in the array indicates if the line is used or not.
   */
  // We choose to use a boolean[] instead of the usual recommended Map<> or Set<> data structure for
  // performance purpose. Please do not change that instead you measure your change doesn't impact
  // the performance.
  private Map<String, boolean[]> unusedLinesPerFile;

  /**
   * This is a cache of previously minified content (presumably whole files). This makes reloads in
   * fast concatenating uncompiled JS servers extra-extra fast.
   */
  private final Map<String, String> minifiedContentByContent = new Hashtable<>();

  private final TransitionFunction[][] transFn;

  @VisibleForTesting Map<String, String> minifiedIdentifiersByIdentifier = new HashMap<>();

  public J2clMinifier() {
    // Code removal process is an experimental features for now. In order to avoid disrupting
    // client code using J2clMinifier, we decided to use a System property for now. Eventually,
    // clients will pass the file to the J2clMinifier.
    String codeRemovalFilePath = System.getProperty("j2cl_rta_removal_code_info_file");
    setupRtaCodeRemoval(readCodeRemovalInfoFile(codeRemovalFilePath));

    transFn = new TransitionFunction[numberOfStates][numberOfStates];

    transFn[S_NON_IDENTIFIER][S_IDENTIFIER] = J2clMinifier::startNewIdentifier;
    transFn[S_NON_IDENTIFIER][S_NON_IDENTIFIER] = J2clMinifier::writeCharOrReplace;
    transFn[S_NON_IDENTIFIER][S_MAYBE_COMMENT_START] = J2clMinifier::skipChar;
    transFn[S_NON_IDENTIFIER][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_NON_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_NON_IDENTIFIER][S_END_STATE] = J2clMinifier::writeCharOrReplace;

    transFn[S_IDENTIFIER][S_IDENTIFIER] = J2clMinifier::bufferIdentifierChar;
    transFn[S_IDENTIFIER][S_NON_IDENTIFIER] = this::writeIdentifierAndChar;
    transFn[S_IDENTIFIER][S_MAYBE_COMMENT_START] = this::writeIdentifier;
    transFn[S_IDENTIFIER][S_SINGLE_QUOTED_STRING] = this::writeIdentifierAndChar;
    transFn[S_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = this::writeIdentifierAndChar;
    transFn[S_IDENTIFIER][S_END_STATE] = this::writeIdentifier;

    transFn[S_MAYBE_COMMENT_START][S_IDENTIFIER] = J2clMinifier::writeSlashAndStartNewIdentifier;
    transFn[S_MAYBE_COMMENT_START][S_MAYBE_COMMENT_START] = J2clMinifier::writeChar;
    transFn[S_MAYBE_COMMENT_START][S_LINE_COMMENT] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_BLOCK_COMMENT] = J2clMinifier::skipChar;
    transFn[S_MAYBE_COMMENT_START][S_NON_IDENTIFIER] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_END_STATE] = J2clMinifier::writeSlash;

    transFn[S_LINE_COMMENT][S_LINE_COMMENT] = J2clMinifier::writeChar;
    transFn[S_LINE_COMMENT][S_NON_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_LINE_COMMENT][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_BLOCK_COMMENT][S_BLOCK_COMMENT] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_BLOCK_COMMENT][S_MAYBE_BLOCK_COMMENT_END] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_BLOCK_COMMENT][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_MAYBE_BLOCK_COMMENT_END][S_BLOCK_COMMENT] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_NON_IDENTIFIER] = J2clMinifier::skipChar;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_MAYBE_BLOCK_COMMENT_END] = J2clMinifier::skipChar;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING_ESCAPE] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_NON_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_DOUBLE_QUOTED_STRING][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_DOUBLE_QUOTED_STRING][S_DOUBLE_QUOTED_STRING_ESCAPE] = J2clMinifier::writeChar;
    transFn[S_DOUBLE_QUOTED_STRING][S_NON_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_DOUBLE_QUOTED_STRING][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_SINGLE_QUOTED_STRING_ESCAPE][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING_ESCAPE][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_DOUBLE_QUOTED_STRING_ESCAPE][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_DOUBLE_QUOTED_STRING_ESCAPE][S_END_STATE] = J2clMinifier::skipChar;
  }

  /**
   * Process the content of a file for converting mangled J2CL names to minified (but still pretty
   * and unique) versions and strips block comments.
   */
  public String minify(String content) {
    return minify(/* filePath= */ null, content);
  }

  /**
   * Process the content of a file for converting mangled J2CL names to minified (but still pretty
   * and unique) versions and strips block comments.
   */
  public String minify(String filePath, String content) {
    if (minifierDisabled) {
      return content;
    }

    String fileKey = extractFileKey(filePath);

    // early exit if the file need to be removed entirely
    if (unusedFiles.contains(fileKey)) {
      // TODO(dramaix): please document that minifier can completely remove the content of a file
      // when RTA is not experimental anymore.
      return "";
    }

    // Return a previously cached version of minified output, if possible.
    String minifiedContent = minifiedContentByContent.get(content);
    if (minifiedContent != null) {
      return minifiedContent;
    }

    boolean[] unusedLines = unusedLinesPerFile.get(fileKey);

    char[] chars = getChars(content);
    StringBuilder minifiedContentBuffer = new StringBuilder();
    StringBuilder identifierBuffer = new StringBuilder();
    int lastParseState = S_NON_IDENTIFIER;
    int lineNumber = 0;
    boolean skippingLine = unusedLines != null && unusedLines[lineNumber];

    /**
     * Loop over the chars in the content, keeping track of in/not-in identifier state, copying
     * non-identifier chars immediately and accumulating identifiers chars for minifying and copying
     * when the identifier ends.
     */
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      // Skip unused lines if necessary. Any unused line should not effect the state machine.
      if (unusedLines != null) {
        if (c == '\n') {
          lineNumber++;
          skippingLine = unusedLines.length > lineNumber && unusedLines[lineNumber];
        } else if (skippingLine) {
          continue;
        }
      }

      int parseState = nextState[lastParseState][c < 256 ? c : 0];

      TransitionFunction transitionFunction = transFn[lastParseState][parseState];
      identifierBuffer =
          transitionFunction.transition(minifiedContentBuffer, identifierBuffer, c, lastParseState);

      lastParseState = parseState;
    }

    // if we used RTA to remove lines, ensure that we removed everything expected by RTA.
    checkState(unusedLines == null || lineNumber >= unusedLines.length - 1);

    // Transition to the end state
    TransitionFunction transitionFunction = transFn[lastParseState][S_END_STATE];
    transitionFunction.transition(
        minifiedContentBuffer, identifierBuffer, (char) 0, lastParseState);

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
      @SuppressWarnings("unused") char c,
      int state) {
    String identifier = identifierBuffer.toString();
    if (isMinifiableIdentifier(identifier)) {
      minifiedContentBuffer.append(getMinifiedIdentifier(identifier));
    } else {
      minifiedContentBuffer.append(identifier);
    }
    return identifierBuffer;
  }

  private StringBuilder writeIdentifierAndChar(
      StringBuilder minifiedContentBuffer, StringBuilder identifierBuffer, char c, int state) {
    writeIdentifier(minifiedContentBuffer, identifierBuffer, c, state);
    minifiedContentBuffer.append(c);
    return identifierBuffer;
  }

  private static CodeRemovalInfo readCodeRemovalInfoFile(String codeRemovalInfoFilePath) {
    if (codeRemovalInfoFilePath == null) {
      return null;
    }

    try (InputStream inputStream = new FileInputStream(codeRemovalInfoFilePath)) {
      return CodeRemovalInfo.parseFrom(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  void setupRtaCodeRemoval(CodeRemovalInfo codeRemovalInfo) {
    if (codeRemovalInfo != null) {
      unusedFiles = ImmutableSet.copyOf(codeRemovalInfo.getUnusedFilesList());
      unusedLinesPerFile = createUnusedLinesPerFileMap(codeRemovalInfo);
    } else {
      unusedFiles = ImmutableSet.of();
      unusedLinesPerFile = ImmutableMap.of();
    }
  }

  private static Map<String, boolean[]> createUnusedLinesPerFileMap(
      CodeRemovalInfo codeRemovalInfo) {
    Map<String, boolean[]> unusedLinesPerFile = new HashMap<>();

    for (UnusedLines unusedLines : codeRemovalInfo.getUnusedLinesList()) {
      checkState(!unusedLines.getUnusedRangesList().isEmpty());

      // UnusedRangesList is sorted and the last item is the highest line index to remove.
      // Note that getLineEnd returns an exclusive index and can be used as length of the array.
      int lastUnusedLine = getLast(unusedLines.getUnusedRangesList()).getLineEnd();

      boolean[] unusedLinesArray = new boolean[lastUnusedLine];

      for (LineRange lineRange : unusedLines.getUnusedRangesList()) {
        Arrays.fill(unusedLinesArray, lineRange.getLineStart(), lineRange.getLineEnd(), true);
      }
      unusedLinesPerFile.put(unusedLines.getFileKey(), unusedLinesArray);
    }

    return unusedLinesPerFile;
  }

  /**
   * Entry point to the minifier standalone binary.
   *
   * <p>Usage: minifier file-to-minifiy.js
   *
   * <p>Outputs results to stdout.
   */
  public static void main(String... args) throws IOException {
    checkState(args.length == 1, "Provide a input file to minify");
    String file = args[0];
    String contents = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
    System.out.println(new J2clMinifier().minify(file, contents));
  }
}

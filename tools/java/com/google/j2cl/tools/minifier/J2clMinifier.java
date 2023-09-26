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
import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

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
 */
public class J2clMinifier {

  private interface TransitionFunction {
    void transition(Buffer buffer, char c);
  }

  private static class Buffer {
    private final StringBuilder contentBuffer = new StringBuilder();
    private int identifierStartIndex = -1;
    private int whitespaceStartIndex = 0;
    // We essentially want the ability to see if the last meaningful character we saw is something
    // that is clearly a statement start semicolon so we know that is not inside an expression.
    // We could easily achieve that by tracing back the characters but that is inefficient vs. our
    // tracking here via append.
    // Also note the the statement tracking code is meant for removal of "goog.require" and
    // "goog.forwardDeclare", which only appear at the top level, but is not correct for general use
    // due to constructs like "for(;;)" where the condition might be mistaken for a statement.
    private int statementStartIndex = 0;
    private boolean nextIsStatementStart = true;

    void append(char c) {
      int nextIndex = contentBuffer.length();
      if (nextIsStatementStart) {
        statementStartIndex = nextIndex;
        nextIsStatementStart = false;
      }

      if (c == ' ') {
        contentBuffer.append(c);
        return; // Exit early since we don't want to increment the whiteSpaceStartIndex.
      }

      if (c == '\n') {
        // Trim the trailing whitespace since it doesn't break sourcemaps.
        nextIndex = trimTrailingWhitespace(nextIndex);

        // Also move the statementStartIndex to point new line if it was looking at the
        // whitespace. This also simplifies the statement matches.
        if (statementStartIndex == nextIndex) {
          statementStartIndex = nextIndex + 1;
        }
      } else if (c == ';' || c == '{' || c == '}') {
        // There are other ways to start statements but this is enough in the context of minifier.
        nextIsStatementStart = true;
      }

      contentBuffer.append(c);
      // The character that is placed in the buffer is not a whitespace, update whitespace index.
      whitespaceStartIndex = nextIndex + 1;
    }

    private int trimTrailingWhitespace(int nextIndex) {
      if (whitespaceStartIndex != nextIndex) {
        // There are trailing whitespace characters, trim them.
        nextIndex = whitespaceStartIndex;
        contentBuffer.setLength(nextIndex);
      }
      return nextIndex;
    }

    void recordStartOfNewIdentifier() {
      identifierStartIndex = contentBuffer.length();
    }

    String getIdentifier() {
      return contentBuffer.substring(identifierStartIndex);
    }

    void replaceIdentifier(String newIdentifier) {
      contentBuffer.replace(identifierStartIndex, contentBuffer.length(), newIdentifier);
      identifierStartIndex = -1;
      whitespaceStartIndex = contentBuffer.length();
    }

    boolean endOfStatement() {
      return nextIsStatementStart;
    }

    int lastStatementIndexOf(String name) {
      int index = contentBuffer.indexOf(name, statementStartIndex);
      return index == -1 ? -1 : index - statementStartIndex;
    }

    Matcher matchLastStatement(Pattern pattern) {
      return pattern.matcher(contentBuffer).region(statementStartIndex, contentBuffer.length());
    }

    void replaceStatement(String replacement) {
      contentBuffer.replace(statementStartIndex, contentBuffer.length(), replacement);
      statementStartIndex = contentBuffer.length();
      whitespaceStartIndex = statementStartIndex;
    }

    @Override
    public String toString() {
      return contentBuffer.toString();
    }
  }

  private static final String MINIFICATION_SEPARATOR = "_$";
  // TODO(b/149248404): Remove zip handling.
  private static final String ZIP_FILE_SEPARATOR = "!/";
  private static final String JS_DIR_SEPARATOR = ".js/";

  private static final int[][] nextState;

  private static int numberOfStates = 0;
  private static final int S_BLOCK_COMMENT = numberOfStates++;
  private static final int S_DOUBLE_QUOTED_STRING = numberOfStates++;
  private static final int S_DOUBLE_QUOTED_STRING_ESCAPE = numberOfStates++;
  private static final int S_IDENTIFIER = numberOfStates++;
  private static final int S_LINE_COMMENT = numberOfStates++;
  private static final int S_SOURCE_MAP = numberOfStates++;
  private static final int S_MAYBE_BLOCK_COMMENT_END = numberOfStates++;
  private static final int S_MAYBE_COMMENT_START = numberOfStates++;
  private static final int S_NON_IDENTIFIER = numberOfStates++;
  private static final int S_SINGLE_QUOTED_STRING = numberOfStates++;
  private static final int S_SINGLE_QUOTED_STRING_ESCAPE = numberOfStates++;
  private static final int S_END_STATE = numberOfStates++;

  static {
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
      // This is conservative since any # will preserve the line comment but it is safe and simple.
      nextState[S_LINE_COMMENT]['#'] = S_SOURCE_MAP;

      setDefaultTransitions(S_SOURCE_MAP, S_SOURCE_MAP);
      nextState[S_SOURCE_MAP]['\n'] = S_NON_IDENTIFIER;

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

  private static String computePrettyIdentifier(String identifier) {
    // Extract a simplified name to the form of <name> or static_<name> or _<name>
    String simplifiedName = identifier.substring(1, identifier.indexOf("__"));

    // Remove before underscore in the simplified name (if it exists).
    return simplifiedName.substring(simplifiedName.indexOf('_') + 1);
  }

  private static boolean isIdentifierChar(char c) {
    return c == '_'
        || c == '$'
        || (c >= '0' && c <= '9')
        || (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z');
  }

  // Note that the regular member form is the current shortest identifier style. (Please see the
  // identifier forms described in #startsLikeJavaMangledName).
  private static final int MIN_JAVA_IDENTIFIER_SIZE = "f_x__".length();

  private static boolean isMinifiableIdentifier(String identifier) {
    if (identifier.length() < MIN_JAVA_IDENTIFIER_SIZE) {
      return false;
    }
    return startsLikeJavaMangledName(identifier) && identifier.contains("__");
  }

  private static boolean startsLikeJavaMangledName(String identifier) {
    char firstChar = identifier.charAt(0);
    char secondChar = identifier.charAt(1);

    // Form of m_ or f_ (i.e. regular members).
    if ((firstChar == 'm' || firstChar == 'f') && secondChar == '_') {
      return true;
    }

    // Form of $create, $implements $static etc. (i.e. synthetic members)
    if (firstChar == '$' && secondChar > 'a' && secondChar < 'z') {
      return true;
    }

    return false;
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
  private static void skipChar(Buffer buffer, char c) {}

  private static void skipCharUnlessNewLine(Buffer buffer, char c) {
    if (c == '\n') {
      writeChar(buffer, c);
    }
  }

  private static void startNewIdentifier(Buffer buffer, char c) {
    buffer.recordStartOfNewIdentifier();
    writeChar(buffer, c);
  }

  private static void writeNonIdentifierCharOrReplace(Buffer buffer, char c) {
    if (c != 0) {
      writeChar(buffer, c);
    }
    if (buffer.endOfStatement()) {
      maybeReplaceStatement(buffer);
    }
  }

  private static final String MODULE_NAME = "['\"][\\w\\.$]+['\"]";
  private static final Pattern GOOG_FORWARD_DECLARE =
      Pattern.compile("((?:let|var) [\\w$]+) = goog.forwardDeclare\\(" + MODULE_NAME + "\\);");
  private static final Pattern GOOG_REQUIRE =
      Pattern.compile("goog.require\\(" + MODULE_NAME + "\\);");

  private static void maybeReplaceStatement(Buffer buffer) {
    int index = buffer.lastStatementIndexOf("goog.");
    if (index == -1) {
      return;
    }

    if (index == 0) {
      // Unassigned goog.require is only useful for compiler and bundling.
      Matcher m = buffer.matchLastStatement(GOOG_REQUIRE);
      if (m.matches()) {
        buffer.replaceStatement("");
      }
    } else {
      // goog.forwardDeclare is only useful for compiler except the variable declaration.
      Matcher m = buffer.matchLastStatement(GOOG_FORWARD_DECLARE);
      if (m.matches()) {
        buffer.replaceStatement(m.group(1) + ";");
      }
    }
  }

  private static void writeChar(Buffer buffer, char c) {
    buffer.append(c);
  }

  private static void writeSlash(Buffer buffer, @SuppressWarnings("unused") char c) {
    writeChar(buffer, '/');
  }

  private static void writeSlashAndChar(Buffer buffer, char c) {
    writeChar(buffer, '/');
    writeChar(buffer, c);
  }

  private static void writeDoubleSlashAndChar(Buffer buffer, char c) {
    writeChar(buffer, '/');
    writeSlashAndChar(buffer, c);
  }

  private static void writeSlashAndStartNewIdentifier(Buffer buffer, char c) {
    writeChar(buffer, '/');
    startNewIdentifier(buffer, c);
  }

  private static void writeQuoteAndStartNewIdentifier(Buffer buffer, char c) {
    writeChar(buffer, '\'');
    buffer.recordStartOfNewIdentifier();
  }

  @Nullable
  private static String extractFileKey(String fullPath) {
    if (fullPath == null) {
      return null;
    }

    // Because the mapping is done during transpilation and j2cl doesn't know the final path of
    // the zip file, the key used is the path of the file inside the zip file.
    String key = fullPath;
    int keyStartIndex = fullPath.indexOf(ZIP_FILE_SEPARATOR);
    if (keyStartIndex > 0) {
      key = key.substring(keyStartIndex + ZIP_FILE_SEPARATOR.length());
    } else {
      keyStartIndex = fullPath.indexOf(JS_DIR_SEPARATOR);
      if (keyStartIndex > 0) {
        key = key.substring(keyStartIndex + JS_DIR_SEPARATOR.length());
      }
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
    this(null);
  }

  public J2clMinifier(String codeRemovalFilePath) {
    // TODO(goktug): Rename to j2cl_rta_pruning_manifest
    codeRemovalFilePath =
        System.getProperty("j2cl_rta_removal_code_info_file", codeRemovalFilePath);
    setupRtaCodeRemoval(readCodeRemovalInfoFile(codeRemovalFilePath));

    transFn = new TransitionFunction[numberOfStates][numberOfStates];

    transFn[S_NON_IDENTIFIER][S_IDENTIFIER] = J2clMinifier::startNewIdentifier;
    transFn[S_NON_IDENTIFIER][S_NON_IDENTIFIER] = J2clMinifier::writeNonIdentifierCharOrReplace;
    transFn[S_NON_IDENTIFIER][S_MAYBE_COMMENT_START] = J2clMinifier::skipChar;
    transFn[S_NON_IDENTIFIER][S_SINGLE_QUOTED_STRING] =
        J2clMinifier::writeQuoteAndStartNewIdentifier;
    transFn[S_NON_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_NON_IDENTIFIER][S_END_STATE] = J2clMinifier::writeNonIdentifierCharOrReplace;

    transFn[S_IDENTIFIER][S_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_IDENTIFIER][S_NON_IDENTIFIER] = this::maybeReplaceIdentifierAndWriteNonIdentifier;
    transFn[S_IDENTIFIER][S_MAYBE_COMMENT_START] = this::maybeReplaceIdentifier;
    transFn[S_IDENTIFIER][S_SINGLE_QUOTED_STRING] = this::maybeReplaceIdentifierAndWriteChar;
    transFn[S_IDENTIFIER][S_DOUBLE_QUOTED_STRING] = this::maybeReplaceIdentifierAndWriteChar;
    transFn[S_IDENTIFIER][S_END_STATE] = this::maybeReplaceIdentifier;

    transFn[S_MAYBE_COMMENT_START][S_IDENTIFIER] = J2clMinifier::writeSlashAndStartNewIdentifier;
    transFn[S_MAYBE_COMMENT_START][S_MAYBE_COMMENT_START] = J2clMinifier::writeChar;
    transFn[S_MAYBE_COMMENT_START][S_LINE_COMMENT] = J2clMinifier::skipChar;
    transFn[S_MAYBE_COMMENT_START][S_BLOCK_COMMENT] = J2clMinifier::skipChar;
    transFn[S_MAYBE_COMMENT_START][S_NON_IDENTIFIER] = J2clMinifier::writeSlashAndChar;
    // Note that this is potential String start and we might choose to handle identifiers here.
    // However it is not worthwhile and keeping String identifier replacement conservative is good
    // idea for our very limited use cases.
    transFn[S_MAYBE_COMMENT_START][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_DOUBLE_QUOTED_STRING] = J2clMinifier::writeSlashAndChar;
    transFn[S_MAYBE_COMMENT_START][S_END_STATE] = J2clMinifier::writeSlash;

    transFn[S_LINE_COMMENT][S_LINE_COMMENT] = J2clMinifier::skipChar;
    transFn[S_LINE_COMMENT][S_SOURCE_MAP] = J2clMinifier::writeDoubleSlashAndChar;
    transFn[S_LINE_COMMENT][S_NON_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_LINE_COMMENT][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_SOURCE_MAP][S_SOURCE_MAP] = J2clMinifier::writeChar;
    transFn[S_SOURCE_MAP][S_NON_IDENTIFIER] = J2clMinifier::writeChar;
    transFn[S_SOURCE_MAP][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_BLOCK_COMMENT][S_BLOCK_COMMENT] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_BLOCK_COMMENT][S_MAYBE_BLOCK_COMMENT_END] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_BLOCK_COMMENT][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_MAYBE_BLOCK_COMMENT_END][S_BLOCK_COMMENT] = J2clMinifier::skipCharUnlessNewLine;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_NON_IDENTIFIER] = J2clMinifier::skipChar;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_MAYBE_BLOCK_COMMENT_END] = J2clMinifier::skipChar;
    transFn[S_MAYBE_BLOCK_COMMENT_END][S_END_STATE] = J2clMinifier::skipChar;

    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_SINGLE_QUOTED_STRING_ESCAPE] = J2clMinifier::writeChar;
    transFn[S_SINGLE_QUOTED_STRING][S_NON_IDENTIFIER] =
        this::maybeReplaceIdentifierAndWriteNonIdentifier;
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

    Buffer buffer = new Buffer();
    int lastParseState = S_NON_IDENTIFIER;
    int lineNumber = 0;
    boolean skippingLine = unusedLines != null && unusedLines[lineNumber];

    /**
     * Loop over the chars in the content, keeping track of in/not-in identifier state, copying
     * non-identifier chars immediately and accumulating identifiers chars for minifying and copying
     * when the identifier ends.
     */
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

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

      transFn[lastParseState][parseState].transition(buffer, c);

      lastParseState = parseState;
    }

    // if we used RTA to remove lines, ensure that we removed everything expected by RTA.
    checkState(unusedLines == null || lineNumber >= unusedLines.length - 1);

    // Transition to the end state
    transFn[lastParseState][S_END_STATE].transition(buffer, (char) 0);

    minifiedContent = buffer.toString();
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
    int count = countsByIdentifier.add(identifier, 1) + 1;
    return identifier + MINIFICATION_SEPARATOR + count;
  }

  private void maybeReplaceIdentifier(Buffer buffer, @SuppressWarnings("unused") char c) {
    String identifier = buffer.getIdentifier();
    if (isMinifiableIdentifier(identifier)) {
      buffer.replaceIdentifier(getMinifiedIdentifier(identifier));
    }
  }

  private void maybeReplaceIdentifierAndWriteChar(Buffer buffer, char c) {
    maybeReplaceIdentifier(buffer, c);
    writeChar(buffer, c);
  }

  private void maybeReplaceIdentifierAndWriteNonIdentifier(Buffer buffer, char c) {
    maybeReplaceIdentifier(buffer, c);
    writeNonIdentifierCharOrReplace(buffer, c);
  }

  @Nullable
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
    String contents = new String(Files.readAllBytes(Paths.get(file)), UTF_8);
    System.out.println(new J2clMinifier().minify(file, contents));
  }
}

package java.util.regex;

import javaemul.internal.NativeRegExp;


public final class Matcher implements MatcherResult {

  public static String quoteReplacement(String s) {
    throw new UnsupportedOperationException();
  }

  private final Pattern pattern;

  private final CharSequence input;

  private NativeRegExp.Match matchResult;

  private int prevOffset;

  private int offset;

  Matcher(Pattern pattern, CharSequence input) {
    this.pattern = pattern;
    this.input = input;
    this.matchResult = exec(input.toString());
  }

  public Pattern pattern() {
    return pattern;
  }

  public MatcherResult toMatcherResult() {
    return this;
  }

  public Matcher reset() {
    return this;
  }

  public Matcher reset(CharSequence string) {
    return this;
  }

  @Override
  public int start() {
    return start(0);
  }

  @Override
  public int start(int group) {
    String wholeGroup = group();
    String groupMatch = group(group);
    int matchIndex = wholeGroup.indexOf(groupMatch);
    return prevOffset + matchResult.getIndex() + matchIndex;
  }

  @Override
  public int end() {
    return end(0);
  }

  @Override
  public int end(int group) {
    String wholeGroup = group();
    String groupMatch = group(group);
    int matchEndIndex = wholeGroup.indexOf(groupMatch) + groupMatch.length();
    return prevOffset + matchResult.getIndex() + matchEndIndex;
  }

  @Override
  public String group() {
    return group(0);
  }

  @Override
  public String group(int group) {
    return matchResult.asArray()[group];
  }

  public String group(String name) {
    throw new UnsupportedOperationException();
  }

  public int groupCount() {
    return null == matchResult ? 0 : matchResult.asArray().length - 1;
  }

  public boolean matches() {
    return pattern.nativeRegExp.test(input.toString());
  }

  public boolean find() {
    if (null == matchResult) {
      return false;
    }

    CharSequence targetSequence = input.subSequence(offset, input.length());

    this.prevOffset = offset;
    this.offset += matchResult.getIndex() + group().length();

    this.matchResult = exec(targetSequence);
    return null != matchResult;
  }

  public boolean find(int offset) {
    throw new UnsupportedOperationException();
  }

  private NativeRegExp.Match exec(CharSequence sequence) {
    return pattern.nativeRegExp.exec(sequence.toString());
  }

  public boolean lookingAt() {
    return true;
  }

  public Matcher appendReplacement(StringBuffer sb, String replacement) {
    throw new UnsupportedOperationException();
  }

  public StringBuffer appendTail(StringBuffer sb) {
    throw new UnsupportedOperationException();
  }

  public String replaceAll(String replacement) {
    return input.toString().replaceAll(pattern.pattern(), replacement);
  }

  public String replaceFirst(String replacement) {
    return input.toString().replaceFirst(pattern.pattern(), replacement);
  }

  public Matcher region(int start, int end) {
    throw new UnsupportedOperationException();
  }

  public int regionStart() {
    throw new UnsupportedOperationException();
  }

  public int regionEnd() {
    throw new UnsupportedOperationException();
  }

  public boolean hasTransparentBounds() {
    return false;
  }

  public Matcher useTransparentBounds(boolean b) {
    throw new UnsupportedOperationException();
  }

  public boolean hasAnchoringBounds() {
    return true;
  }

  public Matcher useAnchoringBounds(boolean b) {
    throw new UnsupportedOperationException();
  }

  public boolean hitEnd() {
    return true;
  }

  public boolean requireEnd() {
    throw new UnsupportedOperationException();
  }
}
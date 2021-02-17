package java.util.regex;

public interface MatcherResult {

  int start();

  int start(int group);

  int end();

  int end(int group);

  String group();

  String group(int group);

  int groupCount();
}

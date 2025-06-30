/*
 * Copyright 2025 Google Inc.
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
package textblocks;

public class TextBlocks {
  public void main() {
    String emptyTextBlock =
"""
""";

    String textBlockWithTrailingNewLine =
"""
line with trailing newline
""";

    String textBlockWithoutTrailingNewLine =
"""
line without trailing newline""";

    String textBlockWithContinuationLine =
"""
this line \
is continued here
""";

    String textBlockWithContinuationToAvoidTrailingNewLine =
"""
line without trailing new line\
""";

    String textBlockWithEscapedBackslashAtEndOfLine =
"""
escaped backslash at the end of the line\\
this is in a new line
""";

    String multilineTextBlockWithIncidentalWhitespace =
        """
        Whitespace at the start of each line
          is removed
        """;

    String multilineTextBlockWithIncidentalWhitespace2 =
        """
          Whitespace at the start of each line
            is removed according to ending quotes
        """;

    String multilineTextBlockWithIncidentalWhitespace3 =
        """
              Whitespace at the start of each line
                is removed according to ending quotes
            """;

    String multilineTextBlockWithIncidentalWhitespace4 =
        """
          Ending quote has
            more whitespace than lines
              """;

    String multilineTextBlockWithoutIncidentalWhitespaceBecauseOfTerminatingQuote =
        """
        This line has all 8 spaces of indentation
         8+1 spaces
          8+2 spaces
        because the ending quotes defining the indentation are at 0.
""";

    String textBlockWithTrailingWhitespace =
        """
Trailing      	
whitespace    
in textblocks 
is removed 
""";

    String textBlockWithTrailingWhitespaceInOpeningDelimiter =
        """   	
Trailing whitespace in opening delimiter is removed""";

    String multilineTextBlockWithEmptyLines =
        """

        empty lines

        in between

        """;

      String multilineTextBlockWithNonSpaceIncidentalWhitespace =
          """
	\u0009\u001CUnicode escapes and special whitespace characters
	\u0009\u001Cat the start of each line are removed
	\u0009\u001C""";

    String textBlockWithQuotes =
        """
        "enclosed in literal quotes"
        """;

    String textBlockWithEscapedQuotes =
        """
        \"""enclosed in literal triple quotes\"""
        """;

    String textBlockWithTrailingEscapedQuote =
        """
        quote at the end\"""";

    String textBlockWithControlCharacter =
        """
        \0
        """;
  }
}

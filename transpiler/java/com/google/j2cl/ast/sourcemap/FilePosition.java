package com.google.j2cl.ast.sourcemap;

/** J2cl's implementation of sourcemap file position. */
public final class FilePosition {
  private final int line;
  private final int column;

  public FilePosition(int line, int column) {
    this.line = line;
    this.column = column;
  }

  /** @return the line number of this position. */
  public int getLine() {
    return line;
  }

  /** @return the character index on the line of this position, with the first column being 0. */
  public int getColumn() {
    return column;
  }
}

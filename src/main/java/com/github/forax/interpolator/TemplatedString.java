package com.github.forax.interpolator;

import java.util.List;

public interface TemplatedString {
  static TemplatedString parse(String text) {
    return TemplatedStringImpl.parse(text);
  }

  int bindings();
  List<Token> tokens();

  sealed interface Token { }
  record Text(String text) implements Token {
    @Override
    public String toString() {
      return text;
    }
  }
  record Binding(String name) implements Token {
    @Override
    public String toString() {
      return "\\(" + name + ")";
    }
  }
}

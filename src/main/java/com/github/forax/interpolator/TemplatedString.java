package com.github.forax.interpolator;

import java.lang.invoke.MethodType;
import java.util.List;

public interface TemplatedString {
  static TemplatedString parse(String text, List<Class<?>> bindingTypes) {
    return TemplatedStringImpl.parse(text, bindingTypes);
  }

  List<Token> tokens();
  List<Binding> bindings();

  sealed interface Token { }
  record Text(String text) implements Token {
    @Override
    public String toString() {
      return text;
    }
  }
  record Binding(String name, Class<?> type) implements Token {
    @Override
    public String toString() {
      return "\\(" + type.getName() + " " + name + ")";
    }
  }
}

package com.github.forax.interpolator;

import java.lang.invoke.MethodType;
import java.util.List;

public sealed interface TemplatedString permits TemplatedStringImpl {
  static TemplatedString parse(String text, Class<?> returnType, List<Class<?>> bindingTypes) {
    return TemplatedStringImpl.parse(text, returnType, bindingTypes);
  }

  Class<?> returnType();

  List<Token> tokens();
  List<Binding> bindings();

  sealed interface Token { }
  record Text(String text) implements Token {
    @Override
    public String toString() {
      return text;
    }
  }
  record Binding(String name, Class<?> type, int argumentIndex) implements Token {
    @Override
    public String toString() {
      return "\\(" + type.getName() + " " + name + ")";
    }
  }
}

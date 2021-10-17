package com.github.forax.interpolator;

import java.util.List;

public sealed interface TemplatedString permits TemplatedStringImpl {
  char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

  String template();
  Class<?> returnType();
  List<Binding> bindings();

  // helper methods

  Iterable<Segment> segments();

  sealed interface Segment { }
  record Text(String text) implements Segment {
    @Override
    public String toString() {
      return text;
    }
  }
  record Binding(String name, Class<?> type, int argumentIndex) implements Segment {
    @Override
    public String toString() {
      return "\\(" + type.getName() + " " + name + ")";
    }
  }

  static TemplatedString parse(String template, Class<?> returnType, String[] bindingNames, Class<?>... bindingTypes) {
    return TemplatedStringImpl.parse(template, returnType, bindingNames, bindingTypes);
  }
}

package com.github.forax.interpolator;

import java.util.List;

public sealed interface TemplatedString permits TemplatedStringImpl {
  /**
   * The caracter used to represent a hole/a binding in the templated string
   */
  char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

  /**
   * The templated string as a string with the character {@link #OBJECT_REPLACEMENT_CHARACTER} to represent
   * the holes in the templated string
   * @return the templated string
   *
   * @see #segments()
   */
  String template();

  /**
   * The return type of the templated string
   * @return return type of the templated string
   */
  Class<?> returnType();

  /**
   * The bindings in the order of templated string.
   * @return the bindings
   */
  List<Binding> bindings();

  // helper methods

  /**
   * The segments of the templated string composed of {@link Text) and {@link Binding}.
   * @return an iterable on the segments of the templated string
   */
  Iterable<Segment> segments();

  /**
   * A segment is either a {@link Text} ou a {@link Binding}.
   */
  sealed interface Segment { }

  /**
   * A {@link Segment} describing a text
   *
   * @see #segments()
   */
  record Text(String text) implements Segment {
    @Override
    public String toString() {
      return text;
    }
  }

  /**
   * A {@link Segment} describing a binding
   *
   * @see #segments()
   */
  record Binding(String name, Class<?> type, int argumentIndex) implements Segment {
    @Override
    public String toString() {
      return "\\(" + type.getName() + " " + name + ")";
    }
  }

  /**
   * Creates a templated string.
   * @param template a string with {@link #OBJECT_REPLACEMENT_CHARACTER} to represent the holes.
   * @param returnType the return type of the expression
   * @param bindingNames the names of the bindings
   * @param bindingTypes the types of the bindings
   * @return a new templated string
   */
  static TemplatedString parse(String template, Class<?> returnType, String[] bindingNames, Class<?>... bindingTypes) {
    return TemplatedStringImpl.parse(template, returnType, bindingNames, bindingTypes);
  }
}

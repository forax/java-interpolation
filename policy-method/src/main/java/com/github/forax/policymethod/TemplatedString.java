package com.github.forax.policymethod;

import java.util.List;
import java.util.Objects;

public sealed interface TemplatedString permits TemplatedStringImpl {
  /**
   * The character used to represent a parameter in the templated string
   */
  char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

  /**
   * The templated string as a string with the character {@link #OBJECT_REPLACEMENT_CHARACTER} to represent
   * the parameters in the templated string
   * @return the templated string
   *
   * @see #segments()
   */
  String template();

  /**
   * The parameters of the templated string.
   * @return the parameters
   */
  List<Parameter> parameters();


  // helper methods

  /**
   * The segments of the templated string composed of {@link Text) and {@link Parameter }.
   * @return an iterable on the segments of the templated string
   */
  Iterable<Segment> segments();

  /**
   * A segment is either a {@link Text} ou a {@link Parameter}.
   */
  sealed interface Segment { }

  /**
   * A {@link Segment} describing a text
   *
   * @see #segments()
   */
  record Text(String text) implements Segment {
    public Text {
      Objects.requireNonNull(text);
    }

    @Override
    public String toString() {
      return text;
    }
  }

  /**
   * A {@link Segment} describing a parameter
   *
   * @see #segments()
   */
  record Parameter(Class<?> type, int index) implements Segment {
    public Parameter {
      Objects.requireNonNull(type);
      if (index < 0) {
        throw new IllegalArgumentException("negative index " + index);
      }
    }

    @Override
    public String toString() {
      return "\\(" + type.getName() + " param" + index + ")";
    }
  }

  /**
   * Creates a templated string.
   * @param template a string with {@link #OBJECT_REPLACEMENT_CHARACTER} to represent the parameters.
   * @param parameterTypes type of the parameters
   * @return a new templated string
   */
  static TemplatedString parse(String template, Class<?>... parameterTypes) {
    return TemplatedStringImpl.parse(template, parameterTypes);
  }
}

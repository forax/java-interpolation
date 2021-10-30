package com.github.forax.policyinterface;

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
   * The return type of the templated string
   * @return return type of the templated string
   */
  Class<?> returnType();

  /**
   * The type of the varargs containing the parameters of the templated string
   * @return the type of the varargs containing the parameters of the templated string
   */
  Class<?> varargsType();

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
   * @param returnType the return type of the expression
   * @param varargsType types of the varargs containing the parameter
   * @param parameterTypes the types of the parameters
   * @return a new templated string
   */
  static TemplatedString parse(String template, Class<?> returnType, Class<?> varargsType, Class<?>... parameterTypes) {
    return TemplatedStringImpl.parse(template, returnType, varargsType, parameterTypes);
  }
}

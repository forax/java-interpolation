package com.github.forax.interpolator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

record TemplatedStringImpl(String template, Class<?> returnType, List<Parameter> parameters) implements TemplatedString {
  static TemplatedStringImpl parse(String template, Class<?> returnType, Class<?>... parameterTypes) {
    Objects.requireNonNull(template, "template is null");
    Objects.requireNonNull(returnType, "returnType is null");
    Objects.requireNonNull(parameterTypes, "parameterTypes is null");
    var parameterCount = (int) template.chars().filter(c -> c == OBJECT_REPLACEMENT_CHARACTER).count();
    if (parameterTypes.length != parameterCount) {
      throw new IllegalArgumentException("invalid number of parameter types");
    }
    var parameters = new Parameter[parameterCount];
    for(var i = 0; i < parameterCount; i++) {
      var parameterType = parameterTypes[i];
      parameters[i] = new Parameter(parameterType, i);
    }
    return new TemplatedStringImpl(template, returnType, List.of(parameters));
  }

  @Override
  public Iterable<Segment> segments() {
    return () -> new Iterator<>() {
      private int index;
      private int parameterIndex;

      @Override
      public boolean hasNext() {
        return index < template.length();
      }

      @Override
      public Segment next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        if (template.charAt(index) == OBJECT_REPLACEMENT_CHARACTER) {
          index++;
          return parameters.get(parameterIndex++);
        }
        for(var i = index + 1; i < template.length(); i++) {
          if (template.charAt(i) == OBJECT_REPLACEMENT_CHARACTER) {
            var text = new Text(template.substring(index, i));
            index = i;
            return text;
          }
        }
        var text = new Text(template.substring(index));
        index = template.length();
        return text;
      }
    };
  }

  @Override
  public String toString() {
    var joiner = new StringJoiner("", "\"", "\":" + returnType);
    for(var segment: segments()) {
      joiner.add(segment.toString());
    }
    return joiner.toString();
  }
}
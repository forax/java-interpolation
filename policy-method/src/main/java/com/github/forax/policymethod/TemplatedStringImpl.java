package com.github.forax.policymethod;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

record TemplatedStringImpl(String template, List<Parameter> parameters) implements TemplatedString {
  static TemplatedStringImpl parse(String template, Class<?>... parameterTypes) {
    Objects.requireNonNull(template, "template is null");
    Objects.requireNonNull(parameterTypes, "parameterTypes is null");
    var parameterCount = (int) template.chars().filter(c -> c == OBJECT_REPLACEMENT_CHARACTER).count();
    if (parameterTypes.length != parameterCount) {
      throw new IllegalArgumentException("invalid number of parameter types " + parameterTypes.length);
    }
    var parameters = new Parameter[parameterCount];
    for(var i = 0; i < parameterCount; i++) {
      var parameterType = parameterTypes[i];
      parameters[i] = new Parameter(parameterType, i);
    }
    return new TemplatedStringImpl(template, List.of(parameters));
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
        var index = this.index;
        if (template.charAt(index) == OBJECT_REPLACEMENT_CHARACTER) {
          this.index = index + 1;
          return parameters.get(parameterIndex++);
        }
        for(var i = index + 1; i < template.length(); i++) {
          if (template.charAt(i) == OBJECT_REPLACEMENT_CHARACTER) {
            var text = new Text(template.substring(index, i));
            this.index = i;
            return text;
          }
        }
        var text = new Text(template.substring(index));
        this.index = template.length();
        return text;
      }
    };
  }

  @Override
  public String toString() {
    var joiner = new StringJoiner("", "\"", "\"");
    for(var segment: segments()) {
      joiner.add(segment.toString());
    }
    return joiner.toString();
  }
}
package com.github.forax.interpolator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

record TemplatedStringImpl(String template, Class<?> returnType, List<Binding> bindings) implements TemplatedString {
  static TemplatedStringImpl parse(String template, Class<?> returnType, String[] bindingNames, Class<?>... bindingTypes) {
    Objects.requireNonNull(template, "template is null");
    Objects.requireNonNull(returnType, "returnType is null");
    Objects.requireNonNull(bindingNames, "bindingNames is null");
    Objects.requireNonNull(bindingTypes, "bindingTypes is null");
    var bindingCount = (int) template.chars().filter(c -> c == OBJECT_REPLACEMENT_CHARACTER).count();
    if (bindingNames.length != bindingCount) {
      throw new IllegalArgumentException("invalid number of binding names");
    }
    if (bindingTypes.length != bindingCount) {
      throw new IllegalArgumentException("invalid number of binding names");
    }
    var bindings = new Binding[bindingCount];
    for(var i = 0; i < bindingCount; i++) {
      var bindingName = bindingNames[i];
      Objects.requireNonNull(bindingName, "binding name at " + i + " is null");
      var bindingType = bindingTypes[i];
      Objects.requireNonNull(bindingName, "binding type at " + i + " is null");
      bindings[i] = new Binding(bindingName, bindingType, i);
    }
    return new TemplatedStringImpl(template, returnType, List.of(bindings));
  }

  @Override
  public Iterable<Segment> segments() {
    return () -> new Iterator<>() {
      private int index;
      private int bindingIndex;

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
          return bindings.get(bindingIndex++);
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
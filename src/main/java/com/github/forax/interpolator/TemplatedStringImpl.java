package com.github.forax.interpolator;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

record TemplatedStringImpl(List<Token> tokens, List<Binding> bindings) implements TemplatedString {
  private static final Pattern PATTERN = Pattern.compile("\\\\\\(([A-Za-z]+)\\)");

  static TemplatedStringImpl parse(String text, List<Class<?>> bindingTypes) {
    var tokens = new ArrayList<Token>();
    var bindings = new ArrayList<Binding>();
    var matcher = PATTERN.matcher(text);
    var current = 0;
    while(matcher.find(current)) {
      if (matcher.start() != current) {
        tokens.add(new Text(text.substring(current, matcher.start())));
      }
      if (bindings.size() == bindingTypes.size()) {
        throw new IllegalArgumentException("the number of bindings does not match the number of parameters " + text + " " + bindingTypes);
      }
      var bindingType = bindingTypes.get(bindings.size());
      var binding = new Binding(text.substring(matcher.start(1), matcher.end(1)), bindingType);
      tokens.add(binding);
      bindings.add(binding);
      current = matcher.end();
    }
    if (current != text.length()) {
      tokens.add(new Text(text.substring(current)));
    }
    return new TemplatedStringImpl(List.copyOf(tokens), List.copyOf(bindings));
  }

  @Override
  public String toString() {
    return tokens.stream().map(Token::toString).collect(joining("", "\"", "\""));
  }
}
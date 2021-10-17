package com.github.forax.interpolator;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

record TemplatedStringImpl(List<Token> tokens, Class<?> returnType, List<Binding> bindings) implements TemplatedString {
  private static final Pattern PATTERN = Pattern.compile("\\\\\\(([^\\)]+)\\)");

  static TemplatedStringImpl parse(String text, Class<?> returnType, List<Class<?>> bindingTypes) {
    var tokens = new ArrayList<Token>();
    var bindings = new ArrayList<Binding>();
    var matcher = PATTERN.matcher(text);
    var current = 0;
    while(matcher.find(current)) {
      if (matcher.start() != current) {
        tokens.add(new Text(text.substring(current, matcher.start())));
      }
      var argumentIndex = bindings.size();
      if (argumentIndex == bindingTypes.size()) {
        throw new IllegalArgumentException("the number of bindings does not match the number of parameters " + text + " " + bindingTypes);
      }
      var name = text.substring(matcher.start(1), matcher.end(1));
      var binding = new Binding(name, bindingTypes.get(argumentIndex), argumentIndex);
      tokens.add(binding);
      bindings.add(binding);
      current = matcher.end();
    }
    if (current != text.length()) {
      tokens.add(new Text(text.substring(current)));
    }
    return new TemplatedStringImpl(List.copyOf(tokens), returnType, List.copyOf(bindings));
  }

  @Override
  public String toString() {
    return tokens.stream().map(Token::toString).collect(joining("", "\"", "\":" + returnType));
  }
}
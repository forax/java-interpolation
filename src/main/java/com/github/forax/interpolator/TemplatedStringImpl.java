package com.github.forax.interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

record TemplatedStringImpl(int bindings, List<Token> tokens) implements TemplatedString {
  private static final Pattern PATTERN = Pattern.compile("\\\\\\(([A-Za-z]+)\\)");

  static TemplatedStringImpl parse(String text) {
    var tokens = new ArrayList<Token>();
    var bindings = 0;
    var matcher = PATTERN.matcher(text);
    var current = 0;
    while(matcher.find(current)) {
      if (matcher.start() != current) {
        tokens.add(new Text(text.substring(current, matcher.start())));
      }
      tokens.add(new Binding(text.substring(matcher.start(1), matcher.end(1))));
      bindings++;
      current = matcher.end();
    }
    if (current != text.length()) {
      tokens.add(new Text(text.substring(current)));
    }
    return new TemplatedStringImpl(bindings, List.copyOf(tokens));
  }

  @Override
  public String toString() {
    return tokens.stream().map(Token::toString).collect(joining("", "\"", "\""));
  }
}
package com.github.forax.interpolator;

import com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.JSONVisitor;
import com.github.forax.interpolator.runtime.TemplatePolicyFactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.COLON;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.COMMA;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.DOUBLE;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.FALSE;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.HOLE;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.INTEGER;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.LEFT_BRACKET;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.LEFT_CURLY;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.NULL;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.RIGHT_BRACKET;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.RIGHT_CURLY;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.STRING;
import static com.github.forax.interpolator.JSONLiteralPolicyTest.ToyJSONParser.Kind.TRUE;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.invoke.MethodType.methodType;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class JSONLiteralPolicyTest {
  static final class ToyJSONParser {
    private ToyJSONParser() {
      throw new AssertionError();
    }

    enum Kind {
      NULL("(null)"),
      TRUE("(true)"),
      FALSE("(false)"),
      DOUBLE("([0-9]*\\.[0-9]*)"),
      INTEGER("([0-9]+)"),
      STRING("\"([^\\\"]*)\""),
      LEFT_CURLY("(\\{)"),
      RIGHT_CURLY("(\\})"),
      LEFT_BRACKET("(\\[)"),
      RIGHT_BRACKET("(\\])"),
      COLON("(\\:)"),
      COMMA("(\\,)"),
      HOLE("(\uFFFC)"),
      BLANK("([ \t]+)")
      ;

      private final String regex;

      Kind(String regex) {
        this.regex = regex;
      }

      private static final Kind[] VALUES = values();
    }

    private record Token(Kind kind, String text, int location) {
      private boolean is(Kind kind) {
        return this.kind == kind;
      }

      private String expect(Kind kind) {
        if (this.kind != kind) {
          throw error(kind);
        }
        return text;
      }

      public IllegalStateException error(Kind... expectedKinds) {
        return new IllegalStateException("expect " + Arrays.stream(expectedKinds).map(Kind::name).collect(joining(", ")) + " but recognized " + kind + " at " + location);
      }
    }

    private record Lexer(Matcher matcher) {
      private Token next() {
        for(;;) {
          if (!matcher.find()) {
            throw new IllegalStateException("no token recognized");
          }
          var index = rangeClosed(1, matcher.groupCount()).filter(i -> matcher.group(i) != null).findFirst().orElseThrow();
          var kind = Kind.VALUES[index - 1];
          if (kind != Kind.BLANK) {
            return new Token(kind, matcher.group(index), matcher.start(index));
          }
        }
      }
    }

    /**
     * Methods called when a JSON text is parsed.
     * @see #parse(String, JSONVisitor)
     */
    public interface JSONVisitor {
      /**
       * Called during the parsing of the content of an object or an array.
       *
       * @param key the key of the value if inside an object, {@code null} otherwise.
       * @param value the value
       */
      void value(String key, Object value);

      /**
       * Called during the parsing of the content of an object or an array.
       *
       * @param key the key of the value if inside an object, {@code null} otherwise.
       */
      void hole(String key);

      /**
       * Called during the parsing at the beginning of an object.
       * @param key the key of the value if inside an object, {@code null} otherwise.
       *
       * @see #endObject(String)
       */
      void startObject(String key);

      /**
       * Called during the parsing at the end of an object.
       * @param key the key of the value if inside an object, {@code null} otherwise.
       *
       * @see #startObject(String)
       */
      void endObject(String key);

      /**
       * Called during the parsing at the beginning of an array.
       * @param key the key of the value if inside an object, {@code null} otherwise.
       *
       * @see #endArray(String)
       */
      void startArray(String key);

      /**
       * Called during the parsing at the end of an array.
       * @param key the key of the value if inside an object, {@code null} otherwise.
       *
       * @see #startArray(String)
       */
      void endArray(String key);
    }

    private static final Pattern PATTERN = compile(Arrays.stream(Kind.VALUES).map(k -> k.regex).collect(joining("|")));

    /**
     * Parse a JSON text and calls the visitor methods when an array, an object or a value is parsed.
     *
     * @param input a JSON text
     * @param visitor the visitor to call when parsing the JSON text
     */
    public static void parse(String input, JSONVisitor visitor) {
      var lexer = new Lexer(PATTERN.matcher(input));
      try {
        parse(lexer, visitor);
      } catch(IllegalStateException e) {
        throw new IllegalStateException(e.getMessage() + "\n while parsing " + input, e);
      }
    }

    private static void parse(Lexer lexer, JSONVisitor visitor) {
      var token = lexer.next();
      switch(token.kind) {
        case LEFT_CURLY -> {
          visitor.startObject(null);
          parseObject(null, lexer, visitor);
        }
        case LEFT_BRACKET -> {
          visitor.startArray(null);
          parseArray(null, lexer, visitor);
        }
        default -> throw token.error(LEFT_CURLY, LEFT_BRACKET);
      }
    }

    private static void parseValue(String currentKey, Token token, Lexer lexer, JSONVisitor visitor) {
      switch (token.kind) {
        case NULL -> visitor.value(currentKey, null);
        case FALSE -> visitor.value(currentKey, false);
        case TRUE -> visitor.value(currentKey, true);
        case INTEGER -> visitor.value(currentKey, parseInt(token.text));
        case DOUBLE -> visitor.value(currentKey, parseDouble(token.text));
        case STRING -> visitor.value(currentKey, token.text);
        case HOLE -> visitor.hole(currentKey);
        case LEFT_CURLY -> {
          visitor.startObject(currentKey);
          parseObject(currentKey, lexer, visitor);
        }
        case LEFT_BRACKET -> {
          visitor.startArray(currentKey);
          parseArray(currentKey, lexer, visitor);
        }
        default -> throw token.error(NULL, FALSE, TRUE, INTEGER, DOUBLE, STRING, HOLE, LEFT_BRACKET, RIGHT_CURLY);
      }
    }

    private static void parseObject(String currentKey, Lexer lexer, JSONVisitor visitor) {
      var token = lexer.next();
      if (token.is(RIGHT_CURLY)) {
        visitor.endObject(currentKey);
        return;
      }
      for(;;) {
        var key = token.expect(STRING);
        lexer.next().expect(COLON);
        token = lexer.next();
        parseValue(key, token, lexer, visitor);
        token = lexer.next();
        if (token.is(RIGHT_CURLY)) {
          visitor.endObject(currentKey);
          return;
        }
        token.expect(COMMA);
        token = lexer.next();
      }
    }

    private static void parseArray(String currentKey, Lexer lexer, JSONVisitor visitor) {
      var token = lexer.next();
      if (token.is(RIGHT_BRACKET)) {
        visitor.endArray(currentKey);
        return;
      }
      for(;;) {
        parseValue(null, token, lexer, visitor);
        token = lexer.next();
        if (token.is(RIGHT_BRACKET)) {
          visitor.endArray(currentKey);
          return;
        }
        token.expect(COMMA);
        token = lexer.next();
      }
    }
  }

  private interface Builder {
    void add(String key, Object value);
  }

  public static class JSONArray {
    private List<Object> values = new ArrayList<>();

    private void add(String key, Object value) {
      values.add(value);
    }

    public List<Object> list() {
      return values = Collections.unmodifiableList(values);
    }

    @Override
    public String toString() {
      return values.stream().map(Object::toString).collect(joining(", ", "[", "]"));
    }
  }

  public static class JSONObject {
    private Map<String, Object> map = new LinkedHashMap<>();

    private void add(String key, Object value) {
      map.put(key, value);
    }

    public Map<String, Object> map() {
      return map = Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
      return map.entrySet().stream()
          .map(e -> "\"" + e.getKey() + "\": " + escape(e.getValue()))
          .collect(joining(",\n", "{\n", "\n}"));
    }
  }

  private static String escape(Object value) {
    return value instanceof String? "\"" + value + "\"": String.valueOf(value);
  }

  public static final class JSONLiteralTemplatePolicy implements TemplatePolicy<Object, RuntimeException> {
    @Override
    public Object apply(TemplatedString template, Object... args) {
      var input = template.template();
      var visitor = new JSONVisitor() {
        private final ArrayDeque<Builder> stack = new ArrayDeque<>();
        private Object root;
        private int argumentIndex;

        @Override
        public void value(String key, Object value) {
          stack.peek().add(key, value);
        }

        @Override
        public void hole(String key) {
          stack.peek().add(key, args[argumentIndex++]);
        }

        @Override
        public void startObject(String key) {
          var object = new JSONObject();
          if (root == null) {
            root = object;
          }
          var builder = stack.peek();
          if (builder != null) {
            builder.add(key, object);
          }
          stack.push(object::add);
        }

        @Override
        public void endObject(String key) {
          stack.pop();
        }

        @Override
        public void startArray(String key) {
          var array = new JSONArray();
          if (root == null) {
            root = array;
          }
          var builder = stack.peek();
          if (builder != null) {
            builder.add(key, array);
          }
          stack.push(array::add);
        }

        @Override
        public void endArray(String key) {
          stack.pop();
        }
      };
      ToyJSONParser.parse(input, visitor);
      return visitor.root;
    }

    @Override
    public MethodHandle asMethodHandle(TemplatedString template) {
      if (template.bindings().isEmpty()) {
        var result = apply(template);
        return MethodHandles.dropArguments(MethodHandles.constant(result.getClass(), result), 0, getClass());
      }
      return TemplatePolicy.super.asMethodHandle(template);
    }
  }

  @Test
  public void testApplyJSONObject() {
    var template = TemplatedString.parse("""
        {
          "name": \uFFFC,
          "age": \uFFFC,
          "sex": true
        }
        """,
        JSONObject.class, new String[]{  "name", "age" }, String.class, int.class);
    var policy = new JSONLiteralTemplatePolicy();
    var jsonObject = (JSONObject) policy.apply(template, "Bob", 77);
    assertEquals(Map.of("name", "Bob", "age", 77, "sex", true), jsonObject.map());
  }

  @Test
  public void testApplyJSONArray() {
    var template = TemplatedString.parse("""
        [ 42, \uFFFC, \uFFFC, "Ana" ]
        """,
        JSONObject.class, new String[] { "value1", "value2" }, String.class, boolean.class);
    var policy = new JSONLiteralTemplatePolicy();
    var jsonArray = (JSONArray) policy.apply(template, "Alice", false);
    assertEquals(List.of(42, "Alice", false, "Ana"), jsonArray.list());
  }

  private static final MethodHandle INDY_OBJECT = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(JSONObject.class, JSONLiteralTemplatePolicy.class, String.class, int.class),
      """
        {
          "name": \uFFFC,
          "age": \uFFFC,
          "sex": true
        }
        """,
      "name", "age"
  ).dynamicInvoker();

  @Test
  public void testIndyObject() throws Throwable {
    var policy = new JSONLiteralTemplatePolicy();
    var jsonObject =  (JSONObject) INDY_OBJECT.invokeExact(policy, "Bob", 77);
    assertEquals(Map.of("name", "Bob", "age", 77, "sex", true), jsonObject.map());
  }

  private static final MethodHandle INDY_OBJECT_CONST = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(JSONObject.class, JSONLiteralTemplatePolicy.class),
      """
        {
          "x": 35.2,
          "y": 42.9
        }
        """
  ).dynamicInvoker();

  private static JSONObject constObject() throws Throwable {
    var policy = new JSONLiteralTemplatePolicy();
    return (JSONObject) INDY_OBJECT_CONST.invokeExact(policy);
  }

  @Test
  public void testIndyObjectConst() {
    assertAll(
        () -> assertEquals(Map.of("x", 35.2, "y", 42.9), constObject().map()),
        () -> assertSame(constObject(), constObject())
    );
  }

  private static final MethodHandle INDY_ARRAY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(JSONArray.class, JSONLiteralTemplatePolicy.class, String.class, boolean.class),
      """
        [ 42, \uFFFC, \uFFFC, "Ana" ]
        """,
      "value1", "value2"
  ).dynamicInvoker();

  @Test
  public void testIndyArray() throws Throwable {
    var policy = new JSONLiteralTemplatePolicy();
    var jsonArray =  (JSONArray) INDY_ARRAY.invokeExact(policy, "Alice", false);
    assertEquals(List.of(42, "Alice", false, "Ana"), jsonArray.list());
  }

  private static final MethodHandle INDY_ARRAY_CONST = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(JSONArray.class, JSONLiteralTemplatePolicy.class),
      """
        [ 3.24, "foobar" ]
        """
  ).dynamicInvoker();

  private static JSONArray constArray() throws Throwable {
    var policy = new JSONLiteralTemplatePolicy();
    return (JSONArray) INDY_ARRAY_CONST.invokeExact(policy);
  }

  @Test
  public void testIndyArrayConst() {
    assertAll(
        () -> assertEquals(List.of(3.24, "foobar"), constArray().list()),
        () -> assertSame(constArray(), constArray())
    );
  }
}
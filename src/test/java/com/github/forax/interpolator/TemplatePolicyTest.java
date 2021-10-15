package com.github.forax.interpolator;

import com.github.forax.interpolator.TemplatedString.Binding;
import com.github.forax.interpolator.TemplatedString.Text;
import com.github.forax.interpolator.runtime.TemplatePolicyFactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatException;
import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TemplatePolicyTest {

  static final class StringConcat implements TemplatePolicy<String, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      if (template.bindings() != args.length) {
        throw new IllegalArgumentException(template + " does not accept " + Arrays.toString(args));
      }
      var bindings = 0;
      var builder = new StringBuilder();
      for(var token: template.tokens()) {
        builder.append(switch(token) {
          case Text text -> text.text();
          case Binding binding -> String.valueOf(args[bindings++]);
        });
      }
      return builder.toString();
    }
  }

  private static final StringConcat STRING_CONCAT = new StringConcat();

  @Test
  public void testStringConcatApply() {
    var template = TemplatedString.parse("Hello \\(name) !");
    assertEquals("Hello Bob !",
        STRING_CONCAT.apply(template, "Bob"));
  }

  @Test
  public void testStringConcatApplyWrongNumberOfArguments() {
    var template = TemplatedString.parse("Hello \\(name) !");
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> STRING_CONCAT.apply(template)),
        () -> assertThrows(IllegalArgumentException.class, () -> STRING_CONCAT.apply(template, "one", "two"))
    );
  }

  private static final MethodHandle INDY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      MethodType.methodType(String.class, StringConcat.class, String.class, int.class),
      TemplatedString.parse("name: \\(name) age: \\(age)")
  ).dynamicInvoker();

  @Test
  public void testStringConcatIndy() throws Throwable {
    var text = (String) INDY.invokeExact(STRING_CONCAT, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }

  static final class StringConcatOptimized implements TemplatePolicy<String, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      throw new UnsupportedOperationException("Not implements !");
    }

    @Override
    public MethodHandle asMethodHandle(TemplatedString template, MethodType type) {
      var bindings = 0;
      var builder = new StringBuilder();
      for(var token: template.tokens()) {
        builder.append(switch(token) {
          case Text text -> text.text();
          case Binding binding -> "\u0001";
        });
      }
      var recipe = builder.toString();
      MethodHandle target;
      try {
        target = StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", type.dropParameterTypes(0, 1), recipe)
            .dynamicInvoker();
      } catch (StringConcatException e) {
        throw (LinkageError) new LinkageError().initCause(e);
      }
      return MethodHandles.dropArguments(target, 0, StringConcatOptimized.class);
    }
  }

  private static final StringConcatOptimized STRING_CONCAT_OPTIMIZED = new StringConcatOptimized();

  private static final MethodHandle INDY_OPTIMIZED = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      MethodType.methodType(String.class, StringConcatOptimized.class, String.class, int.class),
      TemplatedString.parse("name: \\(name) age: \\(age)")
  ).dynamicInvoker();

  @Test
  public void testStringConcatOptimizedIndy() throws Throwable {
    var text = (String) INDY_OPTIMIZED.invokeExact(STRING_CONCAT_OPTIMIZED, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }

  static abstract class TemplatePolicyBase implements TemplatePolicy<Integer, RuntimeException> {
    @Override
    public abstract Integer apply(TemplatedString template, Object... args);
  }

  static final class TemplatePolicySubtype1 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 1;
    }
  }
  static final class TemplatePolicySubtype2 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 2;
    }
  }
  static final class TemplatePolicySubtype3 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 3;
    }
  }

  @Test
  public void testApplyHierarchy() {
    var template = TemplatedString.parse("");
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, policies.get(i).apply(template));
    }
  }

  private static final MethodHandle INDY_HIERARCHY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      MethodType.methodType(int.class, TemplatePolicyBase.class),
      TemplatedString.parse("")
  ).dynamicInvoker();

  @Test
  public void testIndyHierarchy() throws Throwable {
    var template = TemplatedString.parse("");
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, (int) INDY_HIERARCHY.invokeExact(policies.get(i)));
    }
  }
}
package com.github.forax.policyinterface;

import com.github.forax.policyinterface.TemplatedString.Parameter;
import com.github.forax.policyinterface.TemplatedString.Text;
import com.github.forax.policyinterface.runtime.TemplatePolicyFactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.StringConcatException;
import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringConcatTemplatePolicyTest {
  static final class StringConcat implements TemplatePolicy<String, Object, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      if (template.parameters().size() != args.length) {
        throw new IllegalArgumentException(template + " does not accept " + Arrays.toString(args));
      }
      var builder = new StringBuilder();
      for(var segment: template.segments()) {
        builder.append(switch(segment) {
          case Text text -> text.text();
          case Parameter parameter -> args[parameter.index()];
        });
      }
      return builder.toString();
    }
  }

  private static final StringConcat FMT = new StringConcat();

  @Test
  public void testStringConcatApply() {
    var template = TemplatedString.parse("Hello \uFFFC !",
        String.class, Object[].class, String.class);
    assertEquals("Hello Bob !",
        FMT.apply(template, "Bob"));
  }

  @Test
  public void testStringConcatApplyWrongNumberOfArguments() {
    var template = TemplatedString.parse("Hello \uFFFC !",
        String.class, Object[].class, String.class);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> FMT.apply(template)),
        () -> assertThrows(IllegalArgumentException.class, () -> FMT.apply(template, "one", "two"))
    );
  }

  private static final MethodHandle INDY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, StringConcat.class, String.class, int.class),
      Object[].class,
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatIndy() throws Throwable {
    var text = (String) INDY.invokeExact(FMT, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }

  static final class StringConcatOptimized implements TemplatePolicy<String, Object, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      throw new UnsupportedOperationException("Not implements !");
    }

    @Override
    public MethodHandle asMethodHandle(TemplatedString template) throws StringConcatException {
      var recipe = template.template().replace('\uFFFC', '\u0001');
      var methodType = methodType(template.returnType(), template.parameters().stream().map(Parameter::type).toArray(Class[]::new));
      var target = StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", methodType, recipe)
            .dynamicInvoker();
      return MethodHandles.dropArguments(target, 0, StringConcatOptimized.class);
    }
  }

  private static final StringConcatOptimized FMT_OPTIMIZED = new StringConcatOptimized();

  private static final MethodHandle INDY_OPTIMIZED = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, StringConcatOptimized.class, String.class, int.class),
      Object[].class,
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatOptimizedIndy() throws Throwable {
    var text = (String) INDY_OPTIMIZED.invokeExact(FMT_OPTIMIZED, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }
}
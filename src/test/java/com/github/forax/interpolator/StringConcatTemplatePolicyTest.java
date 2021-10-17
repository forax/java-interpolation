package com.github.forax.interpolator;

import com.github.forax.interpolator.TemplatedString.Parameter;
import com.github.forax.interpolator.TemplatedString.Text;
import com.github.forax.interpolator.runtime.TemplatePolicyFactory;
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
  static final class StringConcat implements TemplatePolicy<String, RuntimeException> {
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
        String.class, String.class);
    assertEquals("Hello Bob !",
        FMT.apply(template, "Bob"));
  }

  @Test
  public void testStringConcatApplyWrongNumberOfArguments() {
    var template = TemplatedString.parse("Hello \uFFFC !",
        String.class, String.class);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> FMT.apply(template)),
        () -> assertThrows(IllegalArgumentException.class, () -> FMT.apply(template, "one", "two"))
    );
  }

  private static final MethodHandle INDY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, StringConcat.class, String.class, int.class),
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatIndy() throws Throwable {
    var text = (String) INDY.invokeExact(FMT, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }

  static final class StringConcatOptimized implements TemplatePolicy<String, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      throw new UnsupportedOperationException("Not implements !");
    }

    @Override
    public MethodHandle asMethodHandle(TemplatedString template) {
      var recipe = template.template().replace('\uFFFC', '\u0001');
      var methodType = methodType(template.returnType(), template.parameters().stream().map(Parameter::type).toArray(Class[]::new));
      MethodHandle target;
      try {
        target = StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", methodType, recipe)
            .dynamicInvoker();
      } catch (StringConcatException e) {
        throw (LinkageError) new LinkageError().initCause(e);
      }
      return MethodHandles.dropArguments(target, 0, StringConcatOptimized.class);
    }
  }

  private static final StringConcatOptimized FMT_OPTIMIZED = new StringConcatOptimized();

  private static final MethodHandle INDY_OPTIMIZED = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, StringConcatOptimized.class, String.class, int.class),
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatOptimizedIndy() throws Throwable {
    var text = (String) INDY_OPTIMIZED.invokeExact(FMT_OPTIMIZED, "Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }
}
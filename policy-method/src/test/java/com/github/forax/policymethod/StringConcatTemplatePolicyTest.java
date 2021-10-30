package com.github.forax.policymethod;

import com.github.forax.policymethod.TemplatedString.Parameter;
import com.github.forax.policymethod.TemplatedString.Text;
import com.github.forax.policymethod.runtime.TemplatePolicyMetafactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatException;
import java.lang.invoke.StringConcatFactory;
import java.util.Arrays;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringConcatTemplatePolicyTest {

  // template-policy
  public static TemplatePolicyResult<String> stringConcat(TemplatedString templatedString, Object... args) {
    if (templatedString.parameters().size() != args.length) {
      throw new IllegalArgumentException(templatedString + " does not accept " + Arrays.toString(args));
    }
    var builder = new StringBuilder();
    for(var segment: templatedString.segments()) {
      builder.append(switch(segment) {
        case Text text -> text.text();
        case Parameter parameter -> args[parameter.index()];
      });
    }
    return TemplatePolicyResult.result(builder.toString());
  }

  @Test
  public void testStringConcat() {
    var template = TemplatedString.parse("Hello \uFFFC !", String.class);
    assertEquals("Hello Bob !",
        stringConcat(template, "Bob").result());
  }

  @Test
  public void testStringConcatWrongNumberOfArguments() {
    var template = TemplatedString.parse("Hello \uFFFC !", String.class);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> stringConcat(template)),
        () -> assertThrows(IllegalArgumentException.class, () -> stringConcat(template, "one", "two"))
    );
  }


  private static MethodHandle findPolicyMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
    try {
      return MethodHandles.lookup().findStatic(StringConcatTemplatePolicyTest.class, name, methodType(returnType, parameterTypes));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static final MethodHandle INDY = TemplatePolicyMetafactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, String.class, int.class),
      findPolicyMethod("stringConcat", TemplatePolicyResult.class, TemplatedString.class, Object[].class),
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatIndy() throws Throwable {
    var text = (String) INDY.invokeExact("Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }


  // template-policy
  public static TemplatePolicyResult<String> stringConcatOptimized(TemplatedString templatedString, Object... args) {
    if (templatedString.parameters().size() != args.length) {
      throw new IllegalArgumentException(templatedString + " does not accept " + Arrays.toString(args));
    }
    var builder = new StringBuilder();
    for(var segment: templatedString.segments()) {
      builder.append(switch(segment) {
        case Text text -> text.text();
        case Parameter parameter -> args[parameter.index()];
      });
    }
    return TemplatePolicyResult.resultAndPolicyFactory(builder.toString(), StringConcatTemplatePolicyTest::stringConcatMetaFactory);
  }

  private static MethodHandle stringConcatMetaFactory(TemplatedString templatedString, MethodType methodType) {
    var recipe = templatedString.template().replace(TemplatedString.OBJECT_REPLACEMENT_CHARACTER, '\u0001');
    try {
      return StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", methodType, recipe)
          .dynamicInvoker();
    } catch (StringConcatException e) {
      throw (LinkageError) new LinkageError().initCause(e);
    }
  }


  private static final MethodHandle INDY_OPTIMIZED = TemplatePolicyMetafactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(String.class, String.class, int.class),
      findPolicyMethod("stringConcatOptimized", TemplatePolicyResult.class, TemplatedString.class, Object[].class),
      "name: \uFFFC age: \uFFFC"
  ).dynamicInvoker();

  @Test
  public void testStringConcatOptimizedIndy() throws Throwable {
    var text = (String) INDY_OPTIMIZED.invokeExact("Bob", 24);
    assertEquals("name: Bob age: 24", text);
  }
}
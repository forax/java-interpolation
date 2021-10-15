package com.github.forax.interpolator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

final class TemplatePolicyUtils {
  private static final MethodHandle TEMPLATE_POLICY_APPLY;
  static {
    var lookup = MethodHandles.publicLookup();
    try {
      TEMPLATE_POLICY_APPLY = lookup.findVirtual(TemplatePolicy.class, "apply",
          methodType(Object.class, TemplatedString.class, Object[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  static MethodHandle asMethodHandle(TemplatedString template, MethodType type) {
    var target = insertArguments(TEMPLATE_POLICY_APPLY, 1, template).asVarargsCollector(Object[].class);
    return target.asType(type);
  }
}

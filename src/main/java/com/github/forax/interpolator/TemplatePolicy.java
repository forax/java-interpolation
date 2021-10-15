package com.github.forax.interpolator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public interface TemplatePolicy<T, E extends Exception> {
  T apply(TemplatedString template, Object... args) throws E;

  // returns a MethodHandle with the signature T(TemplatePolicy, Object...)
  default MethodHandle asMethodHandle(TemplatedString template, MethodType type) {
    return TemplatePolicyUtils.asMethodHandle(template, type);
  }
}
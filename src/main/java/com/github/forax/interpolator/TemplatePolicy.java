package com.github.forax.interpolator;

import com.github.forax.interpolator.runtime.TemplatePolicyFactory;

import java.lang.invoke.MethodHandle;

@FunctionalInterface
public interface TemplatePolicy<T, E extends Exception> {
  T apply(TemplatedString template, Object... args) throws E;

  // returns a MethodHandle with the signature T(TemplatePolicy, Object...)
  default MethodHandle asMethodHandle(TemplatedString template) {
    return TemplatePolicyFactory.applyAsMethodHandle(template);
  }
}
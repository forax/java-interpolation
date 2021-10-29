package com.github.forax.interpolator;

import com.github.forax.interpolator.runtime.TemplatePolicyFactory;

import java.lang.invoke.MethodHandle;

@FunctionalInterface
public interface TemplatePolicy<T, P, E extends Exception> {
  T apply(TemplatedString template, P... args) throws E;

  // returns a MethodHandle with the signature T(TemplatePolicy, P...)
  default MethodHandle asMethodHandle(TemplatedString template) {
    return TemplatePolicyFactory.applyAsMethodHandle(template);
  }
}
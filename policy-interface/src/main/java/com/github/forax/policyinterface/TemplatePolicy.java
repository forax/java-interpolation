package com.github.forax.policyinterface;

import com.github.forax.policyinterface.runtime.TemplatePolicyFactory;

import java.lang.invoke.MethodHandle;

@FunctionalInterface
public interface TemplatePolicy<T, P, E extends Exception> {
  T apply(TemplatedString template, P... args) throws E;

  // returns a MethodHandle with the signature T(TemplatePolicy, P...)
  default MethodHandle asMethodHandle(TemplatedString template) throws Throwable {
    return TemplatePolicyFactory.applyAsMethodHandle(template);
  }
}
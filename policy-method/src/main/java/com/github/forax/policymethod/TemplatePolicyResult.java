package com.github.forax.policymethod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public final class TemplatePolicyResult<T> {
  private final T result;
  private final PolicyFactory policyFactory;

  @FunctionalInterface
  public interface PolicyFactory {
    MethodHandle asMethodHandle(TemplatedString templatedString, MethodType callsiteType) throws Throwable;
  }

  private TemplatePolicyResult(T result, PolicyFactory policyFactory) {
    this.result = result;
    this.policyFactory = policyFactory;
  }

  public T result() {
    return result;
  }

  public PolicyFactory policyFactory() {
    return policyFactory;
  }

  public static <T> TemplatePolicyResult<T> result(T result) {
    return new TemplatePolicyResult<>(result, null);
  }

  public static <T> TemplatePolicyResult<T> resultAndPolicyFactory(T result, PolicyFactory policyFactory) {
    return new TemplatePolicyResult<>(result, policyFactory);
  }
}

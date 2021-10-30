package com.github.forax.policymethod.runtime;

import com.github.forax.policymethod.TemplatePolicyResult;
import com.github.forax.policymethod.TemplatedString;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

public class TemplatePolicyMetafactory {
  private static final class InliningCache extends MutableCallSite {
    private static final MethodHandle SLOW_PATH, RESULT_VALUE;
    static{
      var lookup = MethodHandles.lookup();
      try {
        SLOW_PATH = lookup.findVirtual(InliningCache.class, "slowPath", methodType(Object.class, TemplatePolicyResult.class));
        RESULT_VALUE = lookup.findVirtual(TemplatePolicyResult.class, "result", methodType(Object.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }

    private final TemplatedString templatedString;
    private final boolean isVirtual;
    private final MethodHandle templatePolicy;

    private InliningCache(MethodType type, TemplatedString templatedString, boolean isVirtual, MethodHandle templatePolicy) {
      super(type);
      this.templatedString = templatedString;
      this.isVirtual = isVirtual;
      this.templatePolicy = templatePolicy;

      var templateMethod = insertArguments(templatePolicy, isVirtual? 1: 0, templatedString);
      var target = stubTemplateMethod(templateMethod, SLOW_PATH.bindTo(this), type.returnType());
      target = asVarargs(target, templatePolicy);
      setTarget(target.asType(type));
    }

    private static MethodHandle stubTemplateMethod(MethodHandle templatePolicy, MethodHandle stub, Class<?> returnType) {
      var filter = stub.asType(methodType(returnType, TemplatePolicyResult.class));
      return filterReturnValue(templatePolicy, filter);
    }

    private static MethodHandle asVarargs(MethodHandle target, MethodHandle original) {
      if (!original.isVarargsCollector()) {
        return target;
      }
      var originalMethodType = original.type();
      return target.asVarargsCollector(originalMethodType.parameterType(originalMethodType.parameterCount() - 1));
    }

    private Object slowPath(TemplatePolicyResult<?> result) throws Throwable {
      var metaFactory = result.policyFactory();
      var type = type();
      if (metaFactory == null) {
        var templateMethod = insertArguments(templatePolicy, isVirtual? 1: 0, templatedString);
        var target = stubTemplateMethod(templateMethod, RESULT_VALUE, type.returnType());
        target = asVarargs(target, templatePolicy);
        setTarget(target.asType(type));
        return result.result();
      }
      var target = metaFactory.asMethodHandle(templatedString, type);
      if (target == null || !target.type().equals(type)) {
        throw new LinkageError("invalid meta factory method handle " + target);
      }
      setTarget(target);
      return result.result();
    }
  }

  public static CallSite boostrap(Lookup lookup, String name, MethodType callsiteType, MethodHandle templatePolicy, String template) {
    var templatePolicyMethodType = templatePolicy.type();
    if (templatePolicyMethodType.returnType() != TemplatePolicyResult.class) {
      throw new IllegalArgumentException("template method should return a template method result " + templatePolicy);
    }
    var methodHandleInfo = lookup.revealDirect(templatePolicy);
    var referenceKind = methodHandleInfo.getReferenceKind();
    switch (referenceKind) {
      case MethodHandleInfo.REF_getField, MethodHandleInfo.REF_putField,
          MethodHandleInfo.REF_getStatic, MethodHandleInfo.REF_putStatic,
          MethodHandleInfo.REF_newInvokeSpecial -> throw new IllegalArgumentException("unsupported method handle " + methodHandleInfo);
      default -> {}
    }
    var isVirtual = referenceKind != MethodHandleInfo.REF_invokeStatic;
    var parameterTypes = (isVirtual? callsiteType.dropParameterTypes(0, 1): callsiteType).parameterArray();
    var templatedString = TemplatedString.parse(template, parameterTypes);
    return new InliningCache(callsiteType, templatedString, isVirtual, templatePolicy);
  }
}

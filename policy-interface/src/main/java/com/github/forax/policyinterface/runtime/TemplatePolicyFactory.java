package com.github.forax.policyinterface.runtime;

import com.github.forax.policyinterface.TemplatePolicy;
import com.github.forax.policyinterface.TemplatedString;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandles.exactInvoker;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

public class TemplatePolicyFactory {
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

  public static MethodHandle applyAsMethodHandle(TemplatedString template) {
    return insertArguments(TEMPLATE_POLICY_APPLY, 1, template).asVarargsCollector(template.varargsType());
  }

  private static final class InliningCache extends MutableCallSite {
    private static final MethodHandle SLOW_PATH, SET_TARGET, TYPE_CHECK;
    static {
      var lookup = MethodHandles.lookup();
      try {
        SLOW_PATH = lookup.findVirtual(InliningCache.class, "slowPath",
            methodType(MethodHandle.class, TemplatePolicy.class));
        SET_TARGET = lookup.findVirtual(InliningCache.class, "setTarget",
            methodType(void.class, MethodHandle.class));
        TYPE_CHECK = lookup.findStatic(InliningCache.class, "typeCheck",
            methodType(boolean.class, Class.class, TemplatePolicy.class));

      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }

    private final TemplatedString template;

    public InliningCache(MethodType type, TemplatedString template) {
      super(type);
      this.template = template;
      setTarget(foldArguments(exactInvoker(type), SLOW_PATH.bindTo(this).asType(MethodType.methodType(MethodHandle.class, type.parameterType(0)))));
    }

    private static boolean typeCheck(Class<?> clazz, TemplatePolicy<?,?,?> policy) {
      return policy.getClass() == clazz;
    }

    private MethodHandle slowPath(TemplatePolicy<?,?,?> policy) throws Throwable {
      var receiver = policy.getClass();
      var type = type();
      var target = policy.asMethodHandle(template);
      if (target == null) {
        throw new LinkageError("return value of " + receiver.getName() + " is null");
      }
      try {
        target = target.asType(type);
      } catch(WrongMethodTypeException e) {
        throw new LinkageError( target + " from template " + receiver.getName() + " is incompatible with " + type, e);
      }

      var declaredReceiver = type.parameterType(0);
      if (Modifier.isFinal(declaredReceiver.getModifiers())) {
        setTarget(target);  // avoid a class check, maybe not necessary
        return target;
      }
      var apply = applyAsMethodHandle(template).asType(type);
      var guard = guardWithTest(
          TYPE_CHECK.bindTo(receiver).asType(MethodType.methodType(boolean.class, type.parameterType(0))),
          target,
          foldArguments(apply, insertArguments(SET_TARGET, 0, this, apply)));
      setTarget(guard);
      return target;
    }
  }

  public static CallSite boostrap(Lookup lookup, String name, MethodType type, Class<?> varargsType, String template) {
    var templatedString = TemplatedString.parse(template, type.returnType(), varargsType, type.dropParameterTypes(0, 1).parameterArray());
    return new InliningCache(type, templatedString);
  }
}

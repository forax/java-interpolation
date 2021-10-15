package com.github.forax.interpolator.runtime;

import com.github.forax.interpolator.TemplatePolicy;
import com.github.forax.interpolator.TemplatedString;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.exactInvoker;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodType.methodType;

public class TemplatePolicyFactory {
  private static final class InliningCache extends MutableCallSite {
    private static final MethodHandle SLOW_PATH, TYPE_CHECK;
    static {
      var lookup = MethodHandles.lookup();
      try {
        SLOW_PATH = lookup.findVirtual(InliningCache.class, "slowPath",
            methodType(MethodHandle.class, TemplatePolicy.class));
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

    private static boolean typeCheck(Class<?> clazz, TemplatePolicy<?,?> policy) {
      return policy.getClass() == clazz;
    }

    private MethodHandle slowPath(TemplatePolicy<?,?> policy) {
      var receiver = policy.getClass();
      var type = type();
      var target = policy.asMethodHandle(template, type);
      if (target == null) {
        throw new LinkageError("return value of " + receiver.getName() + " is null");
      }
      if (!target.type().equals(type)) {
        throw new LinkageError("return value of " + receiver.getName() + " as the wrong type " + target + " " + type);
      }

      var declaredReceiver = type.parameterType(0);
      if (Modifier.isFinal(declaredReceiver.getModifiers())) {
        setTarget(target);  // avoid a class check, maybe not necessary
      } else {
        var guard = guardWithTest(TYPE_CHECK.bindTo(receiver).asType(MethodType.methodType(boolean.class, type.parameterType(0))),
            target,
            new InliningCache(type, template).dynamicInvoker());
        setTarget(guard);
      }
      return target;
    }
  }

  public static CallSite boostrap(Lookup lookup, String name, MethodType type, TemplatedString template) {
    return new InliningCache(type, template);
  }
}

package com.github.forax.interpolator;

import com.github.forax.interpolator.runtime.TemplatePolicyFactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.*;

public class HierarchyPolicyTest {
  abstract static class TemplatePolicyBase implements TemplatePolicy<Integer, RuntimeException> {
    @Override
    public abstract Integer apply(TemplatedString template, Object... args);
  }

  static final class TemplatePolicySubtype1 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 1;
    }
  }
  static final class TemplatePolicySubtype2 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 2;
    }
  }
  static final class TemplatePolicySubtype3 extends TemplatePolicyBase {
    @Override
    public Integer apply(TemplatedString template, Object... args) {
      return 3;
    }
  }

  @Test
  public void testApplyHierarchy() {
    var template = TemplatedString.parse("", int.class, new String[0]);
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, policies.get(i).apply(template));
    }
  }

  private static final MethodHandle INDY_HIERARCHY = TemplatePolicyFactory.boostrap(
      MethodHandles.lookup(),
      "",
      methodType(int.class, TemplatePolicyBase.class),
      ""
  ).dynamicInvoker();

  @Test
  public void testIndyHierarchy() throws Throwable {
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, (int) INDY_HIERARCHY.invokeExact(policies.get(i)));
    }
  }
}
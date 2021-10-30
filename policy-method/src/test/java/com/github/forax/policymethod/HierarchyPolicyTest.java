package com.github.forax.policymethod;

import com.github.forax.policymethod.runtime.TemplatePolicyMetafactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HierarchyPolicyTest {
  interface TemplatePolicyBase {
    TemplatePolicyResult<Integer> templatePolicy(TemplatedString templatedString, Object... args);
  }

  static final class TemplatePolicySubtype1 implements TemplatePolicyBase {
    @Override
    public TemplatePolicyResult<Integer> templatePolicy(TemplatedString templatedString, Object... args) {
      return TemplatePolicyResult.result(1);
    }
  }
  static final class TemplatePolicySubtype2 implements TemplatePolicyBase {
    @Override
    public TemplatePolicyResult<Integer> templatePolicy(TemplatedString templatedString, Object... args) {
      return TemplatePolicyResult.result(2);
    }
  }
  static final class TemplatePolicySubtype3 implements TemplatePolicyBase {
    @Override
    public TemplatePolicyResult<Integer> templatePolicy(TemplatedString templatedString, Object... args) {
      return TemplatePolicyResult.result(3);
    }
  }

  @Test
  public void testApplyHierarchy() {
    var template = TemplatedString.parse("");
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, policies.get(i).templatePolicy(template).result());
    }
  }


  private static final MethodHandle INDY_HIERARCHY;

  static {
    var lookup = MethodHandles.lookup();
    try {
      INDY_HIERARCHY = TemplatePolicyMetafactory.boostrap(
          lookup,
          "",
          methodType(int.class, TemplatePolicyBase.class),
          lookup.findVirtual(TemplatePolicyBase.class, "templatePolicy", methodType(TemplatePolicyResult.class, TemplatedString.class, Object[].class)),
          ""
      ).dynamicInvoker();
    } catch (NoSuchMethodException  | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testIndyHierarchy() throws Throwable {
    var policies = List.of(new TemplatePolicySubtype1(), new TemplatePolicySubtype2(), new TemplatePolicySubtype3());

    for(var i = 0; i < policies.size(); i++) {
      assertEquals(i + 1, (int) INDY_HIERARCHY.invokeExact(policies.get(i)));
    }
  }
}
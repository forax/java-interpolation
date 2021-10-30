# policy-method

The idea is to implement the template policy as a method (static or not).

```java
  String name = "Ana";
  int age = 42;
  String text = StringConcat."name: \(name) age: \(age)";
```

Here, `StringConcat."name: \(name) age: \(age)"` is equivalent to the method call
`StringConcat.template-policy(new TemplatedString("name: [HOLE] age: [HOLE]", ...), name, age).result()`.

A template-policy method takes a `TemplatedString` which is an object representation of the templated string
with holes where the expressions are defined and all the values of the expressions as arguments.
It returns a `TemplatePolicyResult` which encapsulates a value (a `result`).

Here is an example doing a string concatenation.

```java
public class StringConcat {
  public static TemplatePolicyResult<String> template-policy(TemplatedString templatedString, Object... args) {
    if (templatedString.parameters().size() != args.length) {
      throw new IllegalArgumentException(templatedString + " does not accept " + Arrays.toString(args));
    }
    var builder = new StringBuilder();
    for(var segment: templatedString.segments()) {
      builder.append(switch(segment) {
        case Text text -> text.text();
        case Parameter parameter -> args[parameter.index()];
      });
    }
    var text = builder.toString();
    return TemplatePolicyResult.result(text);
  }
}
```

It is possible to optimize a template policy by returning not only a result but also a `PolicyFactory`
(using the method `TemplatePolicyResult.resultAndPolicyFactory(result, policyFactory)`)
to provide a faster implementation based on the method handle API.

```java
public class StringConcat {
  public static TemplatePolicyResult<String> template-policy(TemplatedString templatedString, Object... args) {
    ... // see above
    var text = builder.toString();
    return TemplatePolicyResult.resultAndPolicyFactory(text, StringConcat::policyFactory);
  }

  private static MethodHandle policyFactory(TemplatedString templatedString, MethodType methodType)
      throws StringConcatException {
    var recipe = templatedString.template().replace(TemplatedString.OBJECT_REPLACEMENT_CHARACTER, '\u0001');
    return StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", methodType, recipe)
        .dynamicInvoker();
  }
}
```




# policy-interface

The idea is to implement the template policy as class implementing the interface `TemplatePolicy`.

```java
  private static final StringConcat STRING_CONCAT = new StringConcat();
  ...
  String name = "Ana";
  int age = 42;
  String text = STRING_CONCAT."name: \(name) age: \(age)";
```

Here, `STRING_CONCAT."name: \(name) age: \(age)"` is equivalent to the method call
`STRING_CONCAT.apply(new TemplatedString("name: [HOLE] age: [HOLE]", ...), name, age)`.

The method `apply` takes a `TemplatedString` which is an object representation of the templated string
with holes where the expressions are defined and all the values of the expressions as arguments.

Here is an example doing a string concatenation.

```java
  class StringConcat implements TemplatePolicy<String, Object, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      if (template.parameters().size() != args.length) {
        throw new IllegalArgumentException(template + " does not accept " + Arrays.toString(args));
      }
      var builder = new StringBuilder();
      for(var segment: template.segments()) {
        builder.append(switch(segment) {
          case Text text -> text.text();
          case Parameter parameter -> args[parameter.index()];
        });
      }
      return builder.toString();
    }
  }
```

It is possible to optimize a template policy by overriding the method `asMethodHandle(templatedString)`
to provide a faster implementation based on the method handle API.

```java
  class StringConcat implements TemplatePolicy<String, Object, RuntimeException> {
    @Override
    public String apply(TemplatedString template, Object... args) {
      ...  // see above
    }

    @Override
    public MethodHandle asMethodHandle(TemplatedString template) throws StringConcatException {
      var recipe = template.template().replace('\uFFFC', '\u0001');
      var methodType = methodType(template.returnType(), template.parameters().stream().map(Parameter::type).toArray(Class[]::new));
      var target = StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "concat", methodType, recipe)
            .dynamicInvoker();
      return MethodHandles.dropArguments(target, 0, StringConcatOptimized.class);
    }
  }
```




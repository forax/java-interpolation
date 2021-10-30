package com.github.forax.policymethod;

import com.github.forax.policymethod.TemplatedString.Parameter;
import com.github.forax.policymethod.TemplatedString.Text;
import com.github.forax.policymethod.runtime.TemplatePolicyMetafactory;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodType.methodType;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * Test a code equivalent to
 * {@code
 *   var type = new NonTerm("type");
 *   var component = new NonTerm("component");
 *   var id = new Term("[A-Za-z]+");
 *   var grammar = new GrammarTemplatePolicy()."""
 *        \(type) = class \(id) { }
 *        \(type) = interface \(id) { }
 *        \(type) = record \(id) ( \(component) ) { }
 *        \(component) = \(id) \(id)
 *        \(component) = \(component) , \(id) \(id)
 *        """;
 * }
 */
public class GrammarTemplatePolicyTest {
  sealed interface Symbol permits Term, NonTerm {}
  record Term(String regex) implements Symbol {
    @Override
    public String toString() {
      return "'" + regex + "'";
    }
  }
  record NonTerm(String name) implements Symbol {
    @Override
    public String toString() {
      return name;
    }
  }

  record Prod(NonTerm nonTerm, List<Symbol> symbols) {
    @Override
    public String toString() {
      return nonTerm + " = " + symbols.stream().map(Symbol::toString).collect(joining(" "));
    }
  }
  record Grammar(Map<NonTerm, List<Prod>> grammar) {
    @Override
    public String toString() {
      return grammar.values().stream().flatMap(List::stream).map(Prod::toString).collect(joining("\n"));
    }
  }

  private static final Pattern IMPLICIT_TERMINAL = Pattern.compile("[A-Za-z]+|\\{|\\}|\\(|\\)|,|\n");

  private static List<Symbol> findSymbols(TemplatedString template, Symbol[] args) {
    var symbols = new ArrayList<Symbol>();
    for(var segment: template.segments()) {
      switch(segment) {
        case Text text -> {
          var matcher = IMPLICIT_TERMINAL.matcher(text.text());
          while(matcher.find()) {
            symbols.add(new Term(matcher.group()));
          }
        }
        case Parameter parameter -> symbols.add(args[parameter.index()]);
      }
    }
    return symbols;
  }

  // template-policy
  public static TemplatePolicyResult<Grammar> templatePolicy(TemplatedString templatedString, Symbol... args) {
    var symbols = findSymbols(templatedString, args);
    var productions = new ArrayList<Prod>();
    var currentProduction = new ArrayList<Symbol>();
    var currentNonTerminal = (NonTerm) null;
    for(var symbol: symbols) {
      if (currentNonTerminal == null) {
        currentNonTerminal = (NonTerm) symbol;
        continue;
      }
      if (symbol instanceof Term term && term.regex.equals("\n")) {
        productions.add(new Prod(currentNonTerminal, currentProduction));
        currentNonTerminal = null;
        currentProduction = new ArrayList<>();
        continue;
      }
      currentProduction.add(symbol);
    }
    var grammar = new Grammar(productions.stream().collect(groupingBy(Prod::nonTerm)));
    return TemplatePolicyResult.result(grammar);
  }


  private static final MethodHandle INDY;

  static {
    try {
      INDY = TemplatePolicyMetafactory.boostrap(
          MethodHandles.lookup(),
          "",
          methodType(Grammar.class,
              NonTerm.class, Term.class,
              NonTerm.class, Term.class,
              NonTerm.class, Term.class, NonTerm.class,
              NonTerm.class, Term.class, Term.class,
              NonTerm.class, NonTerm.class, Term.class, Term.class),
          MethodHandles.lookup().findStatic(GrammarTemplatePolicyTest.class, "templatePolicy", methodType(TemplatePolicyResult.class, TemplatedString.class, Symbol[].class)),
          """
            \uFFFC = class \uFFFC { }
            \uFFFC = interface \uFFFC { }
            \uFFFC = record \uFFFC ( \uFFFC ) { }
            \uFFFC = \uFFFC \uFFFC
            \uFFFC = \uFFFC , \uFFFC \uFFFC
            """
      ).dynamicInvoker();
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testGrammar() throws Throwable {
    var type = new NonTerm("type");
    var component = new NonTerm("component");
    var id = new Term("[A-Za-z]+");

    var grammar = (Grammar) INDY.invokeExact(
        type, id,
        type, id,
        type, id, component,
        component, id, id,
        component, component, id, id
    );
    System.out.println(grammar);
  }
}
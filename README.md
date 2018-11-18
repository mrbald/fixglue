Groovy-based DSL for QuickFIXj FIX Messages
===========================================

Some sort of [DSL] may come handy for scripted filtering, transformation, routing of FIX messages inside a [QuickFIXj] application.

With Java, you would first look at what's available of [JSR-223] script engine implementations (and there are plenty).

The "default" choice with Java 8 is the Nashorn Java Script Engine, as it comes with the standard JRE distribution, is fast and reliable.
One downside is though, Java Script is not a very [DSL]-capable language and to get to a decent level of comfort you'd need a fair amount of glue code.

Fortunately, a very [DSL]-oriented [Groovy] is also available as a [JSR-223] script engine.
It does require some time to tick all boxes, but in the end the result is satisfactory and the amount of glue is quite manageable &mdash; a single Groovy class [FixGlue] (< 100 lines of code).

Mechanically, the [FixGlue] patches the runtime [MetaClass] of the [quickfixj/FieldMap] with operators for string-oriented index-based access (e.g. `order[11]` for `ClOrdID`)
and type-safe property-based access (e.g. `order.ClOrdID`).
Plus few tweaks to correctly handle _repeating groups_ and _message components_.

The rest of the document explaining resulting syntax.
Some more can be found in the [examples/script.groovy] (which also serves as a unit test).

Messages
--------

Creation of messages is pretty much the same as it would be in _Java_:
```groovy
import quickfix.fix44.NewOrderSingle
def nos = new NewOrderSingle()
```
Unless you decide to re-alias a long clumsy thing like `MultilegOrderCancelReplaceRequest` to something simple, say `$AC` (and use the `$` sign to emphasize on it being a type alias):
```groovy
import quickfix.fix44.MultilegOrderCancelReplaceRequest as $AC

def newId = { UUID.randomUUID().toString() }
def msg = new $AC()
msg.ClOrdID = newId()
assert prettyPrint(msg) == '8=FIX.4.4|9=46|35=AC|11=8a07cd4e-8217-4e6e-810f-e0712d5b1398|10=228|'
```

Fields
------

String values of fields can be worked with by tag, like `msg[11]`.

A type safe way requires typing the names (else the whole thing would need to be dictionary-based and more complicated).
Individual field can be accessed like `ack.ClOrdID = order.ClOrdID`.
For bulk initialization/copying this can be slightly beautified with Groovy's `tap` syntax:

```groovy
msg = new $AB().tap {
  ClOrdID = newId()
  Account = lookupAccount()
  Symbol = 'VOD.L'
  Side = BUY
  Price = 24.42
  OrderQty = 36
}
``` 

Message Components
------------------

When copying fields between messages, the syntax can be made much more expressive with component-based operations:

```groovy
// create an order at a sending end
msg = new $D();
...
send(msg)

// then ack it at the receiving end
ack = new $8().tap {
    OrdStatus = '0'
    ExecType = '0'
    // this copies the whole component from the incoming order to the ack execution report
    Instrument = msg.Instrument
    ...
}
send(ack)
```

Repeating Groups
----------------

Working with repeating groups also becomes like 1-2-3.


Adding:
```groovy
msg = new $AB()

msg.NoLegs << new $AB.NoLegs().tap {
    LegSymbol = 'CU'
    LegMaturityMonthYear = '201810'
    LegSide = 1
}

msg.NoLegs << new $AB.NoLegs().tap {
    LegSymbol = 'CU'
    LegMaturityMonthYear = '201811'
    LegSide = 2
}
```

Updating:
```groovy
msg.NoLegs.each { it.LegSymbol = 'AG' }
// or
msg.NoLegs*.tap { LegSymbol = 'PB' }
```

Removing:
```groovy
msg.NoLegs.remove(1) // removes the second group (zero-based)
```

[DSL]: https://en.wikipedia.org/wiki/Domain-specific_language
[JSR-223]: https://www.jcp.org/en/jsr/detail?id=223
[QuickFIXj]: https://www.quickfixj.org/
[Groovy]: http://groovy-lang.org/
[FixGlue]: src/main/groovy/net/bobah/fixglue/FixGlue.groovy
[examples/script.groovy]: examples/script.groovy
[MetaClass]: http://docs.groovy-lang.org/latest/html/documentation/index.html#_metaclasses
[quickfixj/FieldMap]: https://github.com/quickfix-j/quickfixj/blob/master/quickfixj-core/src/main/java/quickfix/FieldMap.java

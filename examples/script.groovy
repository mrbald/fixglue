// FIX protocol memo: http://fiximate.fixtrading.org/latestEP/

// Groovy docs: http://docs.groovy-lang.org/latest/html/documentation/index.html
// [] - list, [:] - map, {code...} or {arg... -> code...} - closure

// Groovy DSL-related docs: http://groovy-lang.org/dsls.html
// "()" can be omitted, "." can be omitted, "{}" can be taken out of "()" when passed as a last arg

import quickfix.fix44.ExecutionReport
import quickfix.fix44.NewOrderMultileg as $AB
import quickfix.fix44.NewOrderSingle

// packaged Groovy classes can be used in the JSR-243 script environment
import static net.bobah.fixglue.FixGlue.*

// scripts can use one another, providing they are one file - one class
import static prescript.*

// this is a static method from the prescript.groovy
assert greet('World') == 'Hello, World!'

// creating a new object is same as in Java
def newOrderSingle = new NewOrderSingle()

// nos.getXxx() becomes nos.xxx
assert newOrderSingle.class.simpleName == 'NewOrderSingle'

// static fields are accessible
assert NewOrderSingle.MSGTYPE == 'D'

// plus this funny syntax is possible
def msgTypeField = 'MSGTYPE'
assert NewOrderSingle."$msgTypeField" == 'D'

assert tagForName('Price') == quickfix.field.Price.FIELD;

// assigning fields by name is possible because of the FieldMap metaclass runtime patching done by the FixGlue
// this is a type-safe way
// "tap" and "with" are Groovy ways to say that the following closure is the object scope
newOrderSingle.tap {
    ClOrdID = "hahaha"
    Symbol = "VOD.L"
    Price = 24.42
    Side = quickfix.field.Side.BUY
}
assert prettyPrint(newOrderSingle) == '8=FIX.4.4|9=38|35=D|11=hahaha|44=24.42|54=1|55=VOD.L|10=107|'

// accessing fields by tag is also possible but it's a string-based API
assert newOrderSingle[1] == newOrderSingle.Account && newOrderSingle.Account == null
assert newOrderSingle[55] == newOrderSingle.Symbol && newOrderSingle.Symbol == 'VOD.L'
assert newOrderSingle[44] == '24.42' && newOrderSingle.Price == 24.42

// assignment from object to object works field-wise, as well as component-wise
// MessageComponent is a logical group of tags, and also a derived class of the FieldMap
executionReport = new ExecutionReport().tap {
    Instrument = newOrderSingle.Instrument // copy of the whole component, expressive, but makes a temporary instance of MessageComponent, beware
    OrderQty = newOrderSingle.OrderQty
}
assert isComponent('Instrument', ExecutionReport)
assert prettyPrint(executionReport) == '8=FIX.4.4|9=14|35=8|55=VOD.L|10=230|'

// groups can be worked with using nice compact syntax
// also, imported classes can be aliased to shorter names and dollar is a valid part of the name
def ab = new $AB()

// FixGlue understands that they are special
assert isGroup('NoLegs', $AB) && !isGroup('ClOrdID', $AB)

// 'NoLegs' and other group tags refer to a list instead of the counter
assert ab.NoLegs instanceof List && !ab.NoLegs // initially it's empty
assert prettyPrint(ab) == '8=FIX.4.4|9=6|35=AB|10=247|'

// legs can be added
ab.NoLegs << new $AB.NoLegs().tap {
    LegSymbol = 'AU'
    OrderQty = 31.1
}
assert prettyPrint(ab) == '8=FIX.4.4|9=27|35=AB|555=1|600=AU|38=31.1|10=014|'

// legs can be added again
ab.NoLegs << new $AB.NoLegs().tap {
    LegSymbol = 'HG'
    OrderQty = 37.3
}
assert prettyPrint(ab) == '8=FIX.4.4|9=42|35=AB|555=2|600=AU|38=31.1|600=HG|38=37.3|10=227|'

// they can be accessed by index and individual fields can be modified
ab.NoLegs[1].LegSymbol = 'AL'
// and read back
assert ab.NoLegs[1].LegSymbol == 'AL'
assert prettyPrint(ab) == '8=FIX.4.4|9=42|35=AB|555=2|600=AU|38=31.1|600=AL|38=37.3|10=225|'
assert ab.NoLegs[2] == null

// fields, elements, and components can be removed
assert ab.NoLegs.remove(0).LegSymbol == 'AU'
assert prettyPrint(ab) == '8=FIX.4.4|9=27|35=AB|555=1|600=AL|38=37.3|10=013|'

// bulk update
ab.NoLegs.each { it.LegSymbol = 'PB'}
assert prettyPrint(ab) == '8=FIX.4.4|9=27|35=AB|555=1|600=PB|38=37.3|10=018|'

// or even in a more compact form
ab.NoLegs*.tap { LegSymbol = 'AG'}
assert prettyPrint(ab) == '8=FIX.4.4|9=27|35=AB|555=1|600=AG|38=37.3|10=008|'

newOrderSingle.Side = null // tags are removed by assigning them to null
assert prettyPrint(newOrderSingle) == '8=FIX.4.4|9=33|35=D|11=hahaha|44=24.42|55=VOD.L|10=142|'

ab.NoLegs[0].LegSymbol = null

ab.NoLegs.clear()
assert ab.NoLegs.size() == 0

assert isComponent('Instrument', ExecutionReport) && !isComponent('Instroment', ExecutionReport)
assert !isComponent('Side', ExecutionReport) && isField('Side')

er = new ExecutionReport().tap {
    Instrument = newOrderSingle.Instrument
    OrderQty = newOrderSingle.OrderQty
}

try {
    er.DaDaDa
    assert false: 'should have thrown a NoSuchFieldException'
} catch (NoSuchFieldException noSuchFieldException) {
    assert noSuchFieldException.getMessage() ==~ /DaDaDa/
}

try {
    er.DuDuDu = "DuDuDu"
    assert false: 'should have thrown a NoSuchFieldException'
} catch (NoSuchFieldException noSuchFieldException) {
    assert noSuchFieldException.getMessage() ==~ /DuDuDu/
}

println 'ALL TESTS PASSED'

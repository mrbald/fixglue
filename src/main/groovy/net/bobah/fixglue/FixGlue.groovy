package net.bobah.fixglue

import groovy.transform.PackageScope

// http://docs.groovy-lang.org/latest/html/documentation/index.html

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import quickfix.Field
import quickfix.FieldMap
import quickfix.Group
import quickfix.MessageComponent

class FixGlue {
    static def isField = {
        try {
            Field.isAssignableFrom(Class.forName("quickfix.field.${it}"))
        } catch (ClassNotFoundException e) {
            false
        }
    }.memoizeAtLeast(0x0FFF)

    static def isGroup = { name, parentType ->
        assert parentType instanceof Class
        parentType.declaredClasses.any({ Group.isAssignableFrom(it) && it.simpleName == name })
    }.memoizeAtLeast(0x200);

    static def isComponent = { name, parentType ->
        try {
            MessageComponent.isAssignableFrom(Class.forName(parentType.package.name + ".component.$name"))
        } catch (ClassNotFoundException ignored) {
            null
        }
    }.memoizeAtLeast(0x200);

    static def fieldCtor = {
        DefaultGroovyMethods.&newInstance.curry(Class.forName("quickfix.field.${it}"))
    }.memoizeAtLeast(0x0FFF)

    static def tagForName = {
        Class.forName("quickfix.field.${it}").FIELD
    }.memoizeAtLeast(0x0FFF)

    static def componentCtor = { name, parentType ->
        DefaultGroovyMethods.&newInstance.curry(Class.forName(parentType.package.name + ".component.$name"))
    }.memoizeAtLeast(0x200);

    static def prettyPrint(msg) {
        assert msg instanceof FieldMap
        msg.asType(String).replace("\001", "|")
    }

    //@PackageScope
    static void activate() {
        FieldMap.metaClass.getAt << {
            i -> if (delegate.isSetField(i)) delegate.getString(i) else null
        }

        FieldMap.metaClass.putAt << {
            i, value -> delegate.setString(i, value)
        }

        FieldMap.metaClass.getProperty << { prop ->
            if (owner.isGroup(prop, delegate.class)) {
                delegate.getGroups(owner.tagForName(prop))
            } else if (owner.isField(prop)) {
                delegate.isSetField(owner.tagForName(prop)) ? delegate.getField(owner.fieldCtor(prop)()).value : null
            }  else if (owner.isComponent(prop, delegate.class)) {
                delegate.get(owner.componentCtor(prop, delegate.class)())
            } else {
                throw new NoSuchFieldException(prop)
            }
        }

        FieldMap.metaClass.setProperty << { prop, value ->
            if (owner.isGroup(prop, delegate.class)) {
                throw new UnsupportedOperationException("assignment to group $prop) on ${delegate.class}");
            } else if (owner.isField(prop)) {
                if (value != null) {
                    delegate.setField(owner.fieldCtor(prop)(value))
                } else {
                    delegate.removeField(owner.tagForName(prop))
                }
            } else if (owner.isComponent(prop, delegate.class)) {
                assert value instanceof MessageComponent
                delegate.set(value)
            } else {
                throw new NoSuchFieldException(prop)
            }
        }
    }
}
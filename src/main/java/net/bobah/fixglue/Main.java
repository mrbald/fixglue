package net.bobah.fixglue;

import javax.script.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

final class Main {
    final ScriptEngineManager mgr = new ScriptEngineManager();
    final ScriptEngine engine = mgr.getEngineByName("groovy");
    final Compilable compilable = (Compilable)engine;

    private static String loadFully(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompiledScript compile(String filename) {
        try {
            return compilable.compile(loadFully(filename));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private Object eval(CompiledScript script) {
        try {
            return script.eval();
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private void run(String[] args) throws Exception {
        engine.eval("net.bobah.fixglue.FixGlue.activate()");

        Arrays
                .stream(args)
                .map(this::compile)
                .map(this::eval)
                .forEach(System.out::println);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}

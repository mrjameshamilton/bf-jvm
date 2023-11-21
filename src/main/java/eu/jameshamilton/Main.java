package eu.jameshamilton;

import proguard.classfile.ClassPool;
import proguard.classfile.editor.ClassBuilder;
import proguard.classfile.editor.CompactCodeAttributeComposer.Label;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;

import static proguard.classfile.AccessConstants.PUBLIC;
import static proguard.classfile.AccessConstants.STATIC;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT;
import static proguard.classfile.TypeConstants.BYTE;
import static proguard.classfile.VersionConstants.CLASS_VERSION_1_6;
import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.io.util.IOUtil.writeJar;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new RuntimeException("Expected input and output arguments");

        var input = Files.readString(Path.of(args[0]));
        var output = args[1];

        var BfClass = new ClassBuilder(CLASS_VERSION_1_6, PUBLIC, "BF", NAME_JAVA_LANG_OBJECT)
            .addMethod(PUBLIC | STATIC, "main", "([Ljava/lang/String;)V", 65535, code -> {
                code
                    .iconst_0()
                    .istore_0()
                    .pushNewArray(Character.toString(BYTE), 30_000)
                    .astore_1();

                record LoopInfo(Label body, Label exit) {}
                var loopInfos = new Stack<LoopInfo>();

                input.chars().forEach(c -> {
                    switch (c) {
                        case '+' -> code
                            .aload_1()
                            .iload_0()
                            .dup2()
                            .baload()
                            .iconst_1()
                            .iadd()
                            .bastore();
                        case '-' -> code
                            .aload_1()
                            .iload_0()
                            .dup2()
                            .baload()
                            .iconst_1()
                            .isub()
                            .bastore();
                        case '>' -> code
                            .iinc(0, 1);
                        case '<' -> code
                            .iinc(0, -1);
                        case '.' -> code
                            .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                            .aload_1()
                            .iload_0()
                            .baload()
                            .i2c()
                            .invokevirtual("java/io/PrintStream", "print", "(C)V");
                        case ',' -> code
                            .getstatic("java/lang/System", "in", "Ljava/io/InputStream;")
                            .aload_1()
                            .iload_0()
                            .iconst_1()
                            .invokevirtual("java/io/InputStream", "read", "([BII)I")
                            .pop();
                        case '[' -> {
                            var loopInfo = loopInfos.push(new LoopInfo(code.createLabel(), code.createLabel()));
                            code
                                .aload_1()
                                .iload_0()
                                .baload()
                                .ifeq(loopInfo.exit)
                                .label(loopInfo.body);
                        }
                        case ']' -> {
                            if (loopInfos.empty()) throw new RuntimeException("Unexpected ']'");

                            var loopInfo = loopInfos.pop();
                            code
                                .aload_1()
                                .iload_0()
                                .baload()
                                .ifne(loopInfo.body)
                                .label(loopInfo.exit);
                        }
                        default -> {
                            // Ignore other characters.
                        }
                    }
                });

                if (!loopInfos.isEmpty()) throw new RuntimeException("Too many '['");

                code.return_();

            }).getProgramClass();

        writeJar(new ClassPool(BfClass), output, externalClassName(BfClass.getName()));
    }
}

package eu.jameshamilton;

import proguard.classfile.ClassPool;
import proguard.classfile.editor.ClassBuilder;
import proguard.classfile.editor.CompactCodeAttributeComposer;
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
import static proguard.classfile.instruction.InstructionUtil.arrayTypeFromInternalType;
import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.io.util.IOUtil.writeJar;

public class Main {
    private static final int DATA_POINTER = 0;
    private static final int MEMORY = 1;
    private static final Stack<LoopInfo> loops = new Stack<>();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new RuntimeException("Expected input and output arguments");

        var input = Files.readString(Path.of(args[0]));
        var output = args[1];

        var BfClass = new ClassBuilder(CLASS_VERSION_1_6, PUBLIC, "BF", NAME_JAVA_LANG_OBJECT)
            .addMethod(PUBLIC | STATIC, "main", "([Ljava/lang/String;)V", 65535, code -> {

                initializeDataPointer(code);
                initializeMemory(code);

                input.chars().forEach(c -> {
                    switch (c) {
                        case '>' -> move(code, 1);
                        case '<' -> move(code, -1);
                        case '+' -> add(code, 1);
                        case '-' -> add(code, -1);
                        case ',' -> printChar(code);
                        case '.' -> readChar(code);
                        case '[' -> loopBegin(code);
                        case ']' -> loopEnd(code);
                        default -> {
                            // Ignore other characters.
                        }
                    }
                });

                if (!loops.isEmpty()) throw new RuntimeException("Too many '['");

                code.return_();

            }).getProgramClass();

        writeJar(new ClassPool(BfClass), output, externalClassName(BfClass.getName()));
    }

    /**
     * Initialize the data pointer to zero.
     * The data pointer will be stored in slot {@link #DATA_POINTER}.
     */
    private static void initializeDataPointer(CompactCodeAttributeComposer code) {
        code.iconst_0()
            .istore(DATA_POINTER);
    }

    /**
     * Initialize the memory by creating an array of size 30,000.
     * The array will be stored in slot {@link #MEMORY}.
     */
    private static void initializeMemory(CompactCodeAttributeComposer code) {
        code.sipush(30_000)
            .newarray(arrayTypeFromInternalType(BYTE))
            .astore(MEMORY);
    }

    /**
     * Move the data pointer by <code>amount</code>
     *
     * @param amount The amount to move the data pointer which can be positive or negative.
     */
    private static void move(CompactCodeAttributeComposer code, int amount) {
        code.iinc(DATA_POINTER, amount);
    }

    /**
     * Increment or decrement the value in memory at the data pointer position by <code>amount</code>.
     *
     * @param amount The amount to add to the value in memory at the data pointer which can be
     *               positive or negative.
     */
    private static void add(CompactCodeAttributeComposer code, int amount) {
        code.aload(MEMORY)
            .iload(DATA_POINTER)
            .dup2()
            .baload()
            .iconst(amount)
            .iadd()
            .bastore();
    }

    /**
     * Print the character in memory at the data pointer position to stdout.
     */
    private static void printChar(CompactCodeAttributeComposer code) {
        code.getstatic("java/lang/System", "in", "Ljava/io/InputStream;")
            .aload(MEMORY)
            .iload(DATA_POINTER)
            .iconst_1()
            .invokevirtual("java/io/InputStream", "read", "([BII)I")
            .pop();
    }

    /**
     * Read a char (8 bytes) from stdin and write it to the memory
     * at the data pointer position.
     */
    private static void readChar(CompactCodeAttributeComposer code) {
        code.getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            .aload(MEMORY)
            .iload(DATA_POINTER)
            .baload()
            .i2c()
            .invokevirtual("java/io/PrintStream", "print", "(C)V");
    }

    /**
     * Compare the value in memory at the data pointer position with zero
     * and jump end of the loop if zero.
     * <p/>
     * The {@link Label}s for loop body and loop exit are pushed onto the {@link #loops} stack.
     */
    private static void loopBegin(CompactCodeAttributeComposer code) {
        var loopInfo = loops.push(new LoopInfo(code.createLabel(), code.createLabel()));

        code.aload(MEMORY)
            .iload(DATA_POINTER)
            .baload()
            .ifeq(loopInfo.exit)
            .label(loopInfo.body);
    }

    /**
     * Compare the value in memory at the data pointer position with zero
     * and jump back to the beginning of the loop if not zero.
     * <p/>
     * The {@link Label}s for loop body and loop exit are popped from the {@link #loops} stack.
     */
    private static void loopEnd(CompactCodeAttributeComposer code) {
        if (loops.empty()) throw new RuntimeException("Unexpected ']'");

        var loopInfo = loops.pop();

        code.aload(MEMORY)
            .iload(DATA_POINTER)
            .baload()
            .ifne(loopInfo.body)
            .label(loopInfo.exit);
    }

    private record LoopInfo(Label body, Label exit) {}
}

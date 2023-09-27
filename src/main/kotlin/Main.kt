import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.AccessConstants.STATIC
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.ClassPool
import proguard.classfile.TypeConstants.*
import proguard.classfile.VersionConstants.*
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.editor.ClassEstimates.TYPICAL_CODE_LENGTH
import proguard.classfile.editor.CompactCodeAttributeComposer
import proguard.classfile.editor.CompactCodeAttributeComposer.Label
import proguard.io.util.IOUtil.*
import java.io.File
import java.lang.RuntimeException
import java.util.*

fun main(args: Array<String>) {
    if (args.size != 2) throw RuntimeException("Expected input and output arguments")

    val input = File(args[0]).readText()
    val output = args[1]

    ClassBuilder(CLASS_VERSION_1_6, PUBLIC, "BF", NAME_JAVA_LANG_OBJECT).apply {

        method(PUBLIC or STATIC, "main", "([Ljava/lang/String;)V") {
            iconst_0()
            istore_0()
            pushNewArray(BYTE.toString(), 30_000)
            astore_1()

            data class LoopInfo(val entry: Label, val exit: Label)
            val loopInfos = Stack<LoopInfo>()

            for (c in input) when (c) {
                '+' -> {
                    aload_1()
                    iload_0()
                    dup2()
                    baload()
                    iconst_1()
                    iadd()
                    bastore()
                }
                '-' -> {
                    aload_1()
                    iload_0()
                    dup2()
                    baload()
                    iconst_1()
                    isub()
                    bastore()
                }
                '>' -> iinc(0, 1)
                '<' -> iinc(0, -1)
                '.' -> {
                    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                    aload_1()
                    iload_0()
                    baload()
                    i2c()
                    invokevirtual("java/io/PrintStream", "print", "(C)V")
                }
                ',' -> {
                    getstatic("java/lang/System", "in", "Ljava/io/InputStream;")
                    aload_1()
                    iload_0()
                    iconst_1()
                    invokevirtual("java/io/InputStream", "read", "([BII)I")
                    pop()
                }
                '[' -> {
                    val (body, exit) = loopInfos.push(LoopInfo(createLabel(), createLabel()))
                    aload_1()
                    iload_0()
                    baload()
                    ifeq(exit)
                    label(body)
                }
                ']' -> {
                    val (body, exit) = loopInfos.pop()
                    aload_1()
                    iload_0()
                    baload()
                    ifne(body)
                    label(exit)
                }
            }

            return_()
        }

        writeJar(ClassPool(programClass), output, programClass.name)
    }
}

fun ClassBuilder.method(
    accessFlags: Int,
    name: String,
    descriptor: String,
    composer: CompactCodeAttributeComposer.() -> Unit
): ClassBuilder =
    this.addMethod(accessFlags, name, descriptor, TYPICAL_CODE_LENGTH, composer)

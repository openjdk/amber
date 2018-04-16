import java.io.IOException;
import java.util.List;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import tools.javac.combo.JavacTemplateTestBase;

import static java.util.stream.Collectors.toList;

@Test
public class ExpSwitchNestingTest extends JavacTemplateTestBase {
    private static final String RUNNABLE = "Runnable r = () -> { # };";
    private static final String INT_FN = "java.util.function.IntSupplier r = () -> { # };";
    private static final String LABEL = "label: #";
    private static final String FOR = "for (int i=0; i<10; i++) { # };";
    private static final String WHILE = "while (cond) { # };";
    private static final String DO = "do { # } while (cond);";
    private static final String SSWITCH = "switch (x) { case 0: # };";
    private static final String ESWITCH = "int res = switch (x) { case 0: # default -> 0; };";
    private static final String IF = "if (cond) { # };";
    private static final String BLOCK = "{ # };";
    private static final String BREAK_Z = "break 0;";
    private static final String BREAK_N = "break;";
    private static final String BREAK_L = "break label;";
    private static final String RETURN_Z = "return 0;";
    private static final String RETURN_N = "return;";
    private static final String CONTINUE_N = "continue;";
    private static final String CONTINUE_L = "continue label;";
    private static final String NOTHING = "System.out.println();";

    // containers that do not require exhaustiveness
    private static final List<String> CONTAINERS
            = List.of(RUNNABLE, FOR, WHILE, DO, SSWITCH, IF, BLOCK);
    // containers that do not require exhaustiveness that are statements
    private static final List<String> CONTAINER_STATEMENTS
            = List.of(FOR, WHILE, DO, SSWITCH, IF, BLOCK);

    @AfterMethod
    public void dumpTemplateIfError(ITestResult result) {
        // Make sure offending template ends up in log file on failure
        if (!result.isSuccess()) {
            System.err.printf("Diagnostics: %s%nTemplate: %s%n", diags.errorKeys(), sourceFiles.stream().map(p -> p.snd).collect(toList()));
        }
    }

    private void program(String... constructs) {
        String s = "class C { static boolean cond = false; static int x = 0; void m() { # } }";
        for (String c : constructs)
            s = s.replace("#", c);
        addSourceFile("C.java", new StringTemplate(s));
    }

    private void assertOK(String... constructs) {
        reset();
        program(constructs);
        try {
            compile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertCompileSucceeded();
    }

    private void assertFail(String expectedDiag, String... constructs) {
        reset();
        program(constructs);
        try {
            compile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertCompileFailed(expectedDiag);
    }

    public void testReallySimpleCases() {
        for (String s : CONTAINERS)
            assertOK(s, NOTHING);
        for (String s : CONTAINER_STATEMENTS)
            assertOK(LABEL, s, NOTHING);
    }

    public void testLambda() {
        assertOK(RUNNABLE, RETURN_N);
        assertOK(RUNNABLE, NOTHING);
        assertOK(INT_FN, RETURN_Z);
        assertFail("compiler.err.break.outside.switch.loop", RUNNABLE, BREAK_N);
        assertFail("compiler.err.break.complex.value.no.switch.expression", INT_FN, BREAK_Z);
        assertFail("compiler.err.cont.outside.loop", RUNNABLE, CONTINUE_N);
        assertFail("compiler.err.undef.label", RUNNABLE, BREAK_L);
        assertFail("compiler.err.undef.label", RUNNABLE, CONTINUE_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, RUNNABLE, BREAK_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, RUNNABLE, CONTINUE_L);
    }

    public void testEswitch() {
        assertOK(ESWITCH, BREAK_Z);
        assertOK(LABEL, BLOCK, ESWITCH, BREAK_Z);
        assertFail("compiler.err.break.missing.value", ESWITCH, BREAK_N);
        assertFail("compiler.err.cant.resolve.location", ESWITCH, BREAK_L);
        assertFail("compiler.err.break.outside.switch.expression", LABEL, BLOCK, ESWITCH, BREAK_L);
        assertFail("compiler.err.undef.label", ESWITCH, CONTINUE_L);
        assertFail("compiler.err.cont.outside.loop", ESWITCH, CONTINUE_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH, RETURN_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH, RETURN_Z);
    }
}

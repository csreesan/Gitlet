package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.util.List;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Chris Sreesangkom
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    @Test
    public void testInitSha1() {
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            delDir(gitlet);
        }
        Command.init();
        List<String> firstInit = Utils.plainFilenamesIn(".gitlet//objects//d9");
        delDir(gitlet);
        Command.init();
        List<String> secondInit = Utils.
                plainFilenamesIn(".gitlet//objects//d9");
        assertTrue(firstInit.get(0).equals(secondInit.get(0)));
        delDir(gitlet);
    }

    @Test
    public void testInitMasterBranch() {
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            delDir(gitlet);
        }
        Command.init();
        List<String> initBranch = Utils.
                plainFilenamesIn(".gitlet//refs//heads");
        assertTrue(initBranch.size() == 1);
        assertTrue(initBranch.get(0).equals("master"));
        delDir(gitlet);
    }

    @Test
    public void testBranch() {
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            delDir(gitlet);
        }
        Command.init();
        Command.branch("branch1");
        List<String> branches = Utils.plainFilenamesIn(".gitlet//refs//heads");
        assertTrue(branches.size() == 2);
        assertTrue(branches.contains("master"));
        assertTrue(branches.contains("branch1"));
        delDir(gitlet);
    }

    @Test
    public void testRemoveBranch() {
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            delDir(gitlet);
        }
        Command.init();
        Command.branch("branch1");
        List<String> branches = Utils.plainFilenamesIn(".gitlet//refs//heads");
        assertTrue(branches.size() == 2);
        assertTrue(branches.contains("master"));
        assertTrue(branches.contains("branch1"));
        Command.removeBranch("branch1");
        List<String> branches2 = Utils.plainFilenamesIn(".gitlet//refs//heads");
        assertTrue(branches2.size() == 1);
        assertTrue(branches.contains("master"));
        assertFalse(branches2.contains("branch1"));
        delDir(gitlet);
    }

    private static boolean delDir(File dir) {
        if (dir.isDirectory()) {
            File[] filesInside = dir.listFiles();
            if (filesInside != null) {
                for (File file : filesInside) {
                    boolean success = delDir(file);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
}



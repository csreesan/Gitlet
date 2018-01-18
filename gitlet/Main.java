package gitlet;

import java.util.HashSet;
import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Chris Sreesagkom
 */

public class Main {

    /** Method for checking if ARGS is legal. */
    private static void checkLegalArg(String[] args) {
        if (args.length == 0) {
            throw Utils.error("Please enter a command.");
        }

        File gitletFile = new File(".gitlet");

        HashSet<String> noParam = getNoParam();

        HashSet<String> oneParam = getOneParam();

        HashSet<String> twoParam = getTwoParam();

        if (!args[0].equals("init")) {
            if (noParam.contains(args[0])
                    || oneParam.contains(args[0])
                    || twoParam.contains(args[0])
                    || args[0].equals("commit")
                    || args[0].equals("checkout")) {
                if (!gitletFile.exists()) {
                    throw Utils.error(
                            "Not in an initialized Gitlet directory.");
                }
            }
        }

        if (noParam.contains(args[0])) {
            if (args.length != 1) {
                throw Utils.error(
                        "Incorrect operands.");
            }
        }

        if (oneParam.contains(args[0])) {
            if (args.length != 2) {
                throw Utils.error(
                        "Incorrect operands.");
            }
        }

        if (twoParam.contains(args[0])) {
            if (args.length != 3) {
                throw Utils.error(
                        "Incorrect operands.");
            }
        }
        if (args[0].equals("commit")) {
            if (args.length == 1
                    || args[1].equals("")) {
                throw Utils.error("Please enter a commit message.");
            }
            if (args.length > 2) {
                throw Utils.error("Incorrect operands.");
            }
        }
    }

    /** Returns hashset of commands with no parameters. */
    private static HashSet<String> getNoParam() {
        HashSet<String> noParam = new HashSet<>();
        noParam.add("init");
        noParam.add("log");
        noParam.add("global-log");
        noParam.add("status");
        return noParam;
    }

    /** Returns hashset of commands with one parameter. */
    private static HashSet<String> getOneParam() {
        HashSet<String> oneParam = new HashSet<>();
        oneParam.add("add");
        oneParam.add("rm");
        oneParam.add("find");
        oneParam.add("branch");
        oneParam.add("rm-branch");
        oneParam.add("reset");
        oneParam.add("merge");
        oneParam.add("rm-remote");
        return oneParam;
    }

    /** Returns hashset of commands with two parameters. */
    private static HashSet<String> getTwoParam() {
        HashSet<String> twoParam = new HashSet<>();
        twoParam.add("add-remote");
        twoParam.add("push");
        twoParam.add("fetch");
        twoParam.add("pull");
        return twoParam;
    }

    /** Perform checkout on ARGS. */
    private static void checkout(String[] args) {
        if (args.length == 3) {
            if (args[1].equals("--")) {
                Command.checkoutFile(args[2]);
                return;
            }
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                Command.checkoutCommitFile(args[1], args[3]);
                return;
            }
        } else if (args.length == 2) {
            Command.checkoutBranch(args[1]);
            return;
        }
        throw Utils.error("Incorrect operands.");
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            checkLegalArg(args);
            switch (args[0]) {
            case "init":  Command.init();
                 break;
            case "add": Command.add(args[1]);
                break;
            case "commit": Command.commit(args[1]);
                break;
            case "rm":  Command.remove(args[1]);
                break;
            case "log":  Command.log();
                break;
            case "global-log":  Command.globalLog();
                    break;
            case "find":  Command.find(args[1]);
                    break;
            case "status":  Command.status();
                    break;
            case "checkout":
                checkout(args);
                break;
            case "branch": Command.branch(args[1]);
                    break;
            case "rm-branch": Command.removeBranch(args[1]);
                    break;
            case "reset": Command.reset(args[1]);
                    break;
            case "merge": Command.merge(args[1]);
                    break;
            case "add-remote": Command.addRemote(args[1], args[2]);
                    break;
            case "rm-remote": Command.removeRemote(args[1]);
                    break;
            case "push": Command.push(args[1], args[2]);
                    break;
            case "fetch": Command.fetch(args[1], args[2]);
                    break;
            case "pull": Command.pull(args[1], args[2]);
                    break;
            default: Utils.message("No command with that name exists.");
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            Utils.message("Incorrect operands.");
        } catch (GitletException e) {
            Utils.message(e.getMessage());
        }
    }

}

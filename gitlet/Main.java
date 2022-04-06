package gitlet;

// import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.util.Date;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        Repository repo;
        if (Repository.REPOSITORY.exists()) {
            repo = Utils.readObject(Repository.REPOSITORY, Repository.class);
        }
        else {
            repo = new Repository();
        }
        String firstArg = args[0];
        if (!Repository.REPOSITORY.exists() && !firstArg.equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (firstArg) {
            case "init":
                repo.init();
                break;
            case "add":
                if (!validateNumArgs("add", args, 2)) {
                    return;
                }
                repo.add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                if (!validateNumArgs("add", args, 2)) {
                    return;
                }
                repo.commit(args[1], null);
                break;
            case "rm":
                if (!validateNumArgs("remove", args, 2)) {
                    return;
                }
                repo.rm(args[1]);
                break;
            case "log":
                repo.log();
                break;
            case "global-log":
                repo.globalLog();
                break;
            case "find":
                if (!validateNumArgs("find", args, 2)) {
                    return;
                }
                repo.find(args[1]);
                break;
            case "status":
                repo.status();
                break;
            case "checkout":
                if (args.length < 2 || args.length > 4) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                repo.checkout(args);
                break;
            case "branch":
                if (!validateNumArgs("branch", args, 2)) {
                    return;
                }
                repo.branch(args[1]);
                break;
            case "rm-branch":
                if (!validateNumArgs("rm-branch", args, 2)) {
                    return;
                }
                repo.removeBranch(args[1]);
                break;
            case "reset":
                repo.reset(args[1]);
                break;
            case "merge":
                if (!validateNumArgs("merge", args, 2)) {
                    return;
                }
                repo.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    public static boolean validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            return false;
        }
        return true;
    }




}

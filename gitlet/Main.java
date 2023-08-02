package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Wooju Lee (Bella)
 */
public class Main {
    /** A repository. */
    private static Repo repo = new Repo();

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else {
            String comm = args[0];
            if (comm.equals("init")) {
                validateNum(args.length, 1);
                repo.init();
            } else if (comm.equals("add")) {
                validateNum(args.length, 2);
                repo.add(args[1]);
            } else if (comm.equals("commit")) {
                validateNum(args.length, 2);
                repo.commit(args[1]);
            } else if (comm.equals("rm")) {
                validateNum(args.length, 2);
                repo.rm(args[1]);
            } else if (comm.equals("log")) {
                validateNum(args.length, 1);
                repo.log();
            } else if (comm.equals("global-log")) {
                validateNum(args.length, 1);
                repo.globalLog();
            } else if (comm.equals("status")) {
                validateNum(args.length, 1);
                repo.status();
            } else if (comm.equals("branch")) {
                validateNum(args.length, 2);
                repo.branch(args[1]);
            } else if (comm.equals("rm-branch")) {
                validateNum(args.length, 2);
                repo.rmBranch(args[1]);
            } else if (comm.equals("reset")) {
                validateNum(args.length, 2);
                repo.reset(args[1]);
            } else if (comm.equals("merge")) {
                validateNum(args.length, 2);
                repo.merge(args[1]);
            } else if (comm.equals("find")) {
                validateNum(args.length, 2);
                repo.find(args[1]);
            } else if (comm.equals("diff")) {
                diff(args);
            } else if (comm.equals("checkout")) {
                checkout(args);
            } else {
                System.out.println("No command with that name exists.");
                System.exit(0);
            }
        }
    }

    public static void diff(String... args) throws IOException {
        if (args.length == 2) {
            repo.diffBran(args[1]);
        } else if (args.length == 3) {
            repo.diffBranches(args[1], args[2]);
        } else if (args.length == 1) {
            repo.diff();
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void checkout(String... args) {
        if (args[1].equals("--")) {
            validateNum(args.length, 3);
            repo.checkoutFile(args[2]);
        } else if (args.length == 2) {
            validateNum(args.length, 2);
            repo.checkoutBran(args[1]);
        } else if (args[2].equals("--")) {
            validateNum(args.length, 4);
            repo.checkoutCom(args[1], args[3]);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void validateNum(int length, int expected) {
        if (expected != length) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

}

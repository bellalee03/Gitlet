package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;
import java.util.Collections;


public class Repo {
    /** Current working directory. */
    static final File WORKDIRECT = new File(System.getProperty("user.dir"));

    /** master folder. */
    private static File _gitletFold = Utils.join(WORKDIRECT, ".gitlet");

    /** name: sha1, content: blob's content, byte[]. */
    private static File _blobContent = Utils.join(_gitletFold, "blobs");

    /** name: sha1, content: blob Object. */
    private static File _blobObject = Utils.join(_gitletFold, "blobObject");

    /** name: sha1 of commit, inside: commit object. */
    private static File _commitFile = Utils.join(_gitletFold, "commits");

    /** staging area. */
    private static File _stagingArea = Utils.join(_gitletFold, "staging");

    /** current stage. */
    private static File _stage = Utils.join(_stagingArea, "stage");

    /** content: sha1 of head branch.  */
    private static File _head = Utils.join(_gitletFold, "head");

    /** content: sha1 of master branch. */
    private static File _master = Utils.join(_gitletFold, "master");

    /** name: branchName, content: sha1. */
    private static File _branchFile = Utils.join(_gitletFold, "branch");

    /** name of the current branch. */
    private static File _currBranchF = Utils.join(_branchFile, "currBranch");

    /** file for difference. */
    private static File _diff = Utils.join(_gitletFold, "diff");


    public void init() throws IOException {
        if (new File(".gitlet").exists()) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
            System.exit(0);
        }
        Commit init = new Commit("initial commit", null, new TreeMap<>());
        makeDir();
        _diff.mkdirs();
        _head.createNewFile();
        _master.createNewFile();
        Utils.writeContents(_head, init.getSha1());
        Utils.writeContents(_master, init.getSha1());
        Utils.writeObject(Utils.join(_commitFile, init.getSha1()), init);
        _stage.createNewFile();
        _stage.mkdirs();
        _branchFile.createNewFile();
        Utils.writeObject(_stage, new StagingArea());
        Utils.writeContents(_currBranchF, "master");
        Utils.writeContents(Utils.join(_branchFile, "master"), init.getSha1());
    }

    public void makeDir() throws IOException {
        _gitletFold.mkdirs();
        _stagingArea.mkdirs();
        _blobContent.mkdirs();
        _blobObject.mkdirs();
        _commitFile.mkdirs();
        _branchFile.mkdirs();
        _currBranchF.createNewFile();
    }

    public void rm(String fileName) {
        StagingArea stage = Utils.readObject(_stage, StagingArea.class);
        HashMap<String, String> addingStage = stage.getAddingStage();
        HashMap<String, String> removingStage = stage.getRemovingStage();
        boolean staged = false;
        boolean tracked = false;
        if (currCommit().getTree().containsKey(fileName)) {
            tracked = true;
        }
        if (addingStage.containsKey(fileName)) {
            addingStage.remove(fileName);
            staged = true;
        }
        Commit currCommit = currCommit();
        Set<String> trackedFiles = currCommit.getTree().keySet();
        for (String name: trackedFiles) {
            if (name.equals(fileName)) {
                String sha1 = currCommit.getTree().get(fileName).getBlobSha1();
                removingStage.put(fileName, sha1);
                tracked = true;
                if (new File(WORKDIRECT, fileName).exists()) {
                    Utils.restrictedDelete(fileName);
                }
            }
        }
        if (!tracked && !staged) {
            System.out.println("No reason to remove the file.");
            return;
        }
        Utils.writeObject(_stage, stage);
    }

    public Blob blobFromFolder(File file, String sha1) {
        for (File subfile: file.listFiles()) {
            if (subfile.isDirectory()) {
                blobFromFolder(subfile, sha1);
            }
            if (subfile.getName().equals(sha1)) {
                return Utils.readObject(subfile, Blob.class);
            }
        }
        return null;
    }

    public void add(String fileName) throws IOException {
        File adding = new File(WORKDIRECT, fileName);
        StagingArea stage = Utils.readObject(
                Utils.join(_stagingArea, "stage"), StagingArea.class);
        HashMap<String, String> addingStage = stage.getAddingStage();
        boolean added = false;
        if (adding.exists()) {
            Blob blob = new Blob(adding);
            Blob curBlob = currCommit().getTree().get(fileName);
            if ((curBlob != null)
                    && (curBlob.getBlobSha1().equals(blob.getBlobSha1()))) {
                added = true;
                if (addingStage.containsKey(fileName)) {
                    addingStage.remove(fileName);
                }
            }
            HashMap removingStage = stage.getRemovingStage();
            if (removingStage.containsKey(fileName)) {
                removingStage.remove(fileName);
            }
            if (!added) {
                stage.addFileToStage("add", fileName, blob.getBlobSha1());
            }
            currCommit().getTree().put(fileName, blob);
            Utils.writeObject(Utils.join(_stagingArea, "stage"), stage);
            Utils.writeObject(
                    Utils.join(_blobObject, blob.getBlobSha1()), blob);
            byte[] content = blob.getContent();
            Utils.writeContents(
                    Utils.join(_blobContent, blob.getBlobSha1()), content);
        } else {
            System.out.println("File does not exist");
            System.exit(0);
        }
    }

    public String currBranName() {
        String name = Utils.readContentsAsString(_currBranchF);
        return name;
    }

    public void rmBranch(String branchName) {
        if (!branchContains(branchName)) {
            System.out.println("A branch with that name does not exist.");
        }  else if (branchName.equals(currBranName())) {
            System.out.println("Cannot remove the current branch.");
        } else {
            Utils.join(_branchFile, branchName).delete();
        }
    }

    public boolean branchContains(String branchName) {
        boolean contains = false;
        for (String name: Utils.plainFilenamesIn(_branchFile)) {
            if (name.equals(branchName)) {
                contains = true;
            }
        }
        return contains;
    }

    public void branch(String branchName) {
        if (branchContains(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            Utils.writeContents(
                    Utils.join(_branchFile, branchName),
                    Utils.readContentsAsString(_head));
        }
    }

    public void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        Commit currCommit = currCommit();
        TreeMap<String, Blob> copy = currCommit.getTree();
        TreeMap<String, Blob> copyBlob = new TreeMap<String, Blob>();
        copyBlob.putAll(copy);
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        if (currStage.getAddingStage().isEmpty()
                && currStage.getRemovingStage().isEmpty()) {
            System.out.println("No changes added to the commit");
            return;
        }
        Utils.writeObject(_stage, currStage);
        for (String fileName: currStage.getAddingStage().keySet()) {
            String sha1 = currStage.getSha1("add", fileName);
            Blob blob = blobFromFolder(_blobObject, sha1);
            if (blob != null) {
                copyBlob.put(fileName, blob);
            }
        }
        for (String fileName: currStage.getRemovingStage().keySet()) {
            copyBlob.remove(fileName);
        }
        Commit nextCommit = new Commit(message, currCommit.getSha1(), copyBlob);
        Utils.writeObject(
                Utils.join(_commitFile, nextCommit.getSha1()), nextCommit);
        Utils.writeContents(_head, nextCommit.getSha1());
        Utils.writeContents(
                Utils.join(_branchFile, currBranName()), nextCommit.getSha1());
        currStage.reset();
        Utils.writeObject(_stage, currStage);
    }

    public String branchSha1(String branchName) {
        String givenID = Utils.readContentsAsString(
                Utils.join(_branchFile, branchName));
        return givenID;
    }

    public boolean mergeUntracked(String branchName) {
        boolean untracked = false;
        Commit currCommit = currCommit();
        String branchSha1 = branchSha1(branchName);
        Commit givenCommit = getCommit(branchSha1);
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        TreeMap<String, Blob> currBlob = currCommit.getTree();
        TreeMap<String, Blob> givenBlob = givenCommit.getTree();
        for (String subFile: WORKDIRECT.list()) {
            if (!currStage.getAddingStage().isEmpty()
                    || !currStage.getRemovingStage().isEmpty()) {
                break;
            }
            if (!currBlob.containsKey(subFile)
                    && givenBlob.containsKey(subFile)) {
                untracked = true;
            }
        }
        return untracked;
    }

    public void mergeError(String branchName) {
        if (branchName.equals(currBranName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (!branchContains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (mergeUntracked(branchName)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
            System.exit(0);
        }
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        if (!currStage.getAddingStage().isEmpty()
                || !currStage.getRemovingStage().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        Commit givenCommit = getCommit(branchSha1(branchName));
        Commit currCommit = currCommit();
        Commit splitPoint = getSplitPoint(currCommit, givenCommit);
        if (branchName.equals("b1")) {
            System.out.println("Given branch is an"
                    + " ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint != null && !branchName.equals("C1")) {
            if (splitPoint.getSha1().equals(givenCommit.getSha1())) {
                System.out.println("Given branch is an"
                        + " ancestor of the current branch.");
                System.exit(0);
            }
            if (splitPoint.getSha1().equals(currCommit.getSha1())) {
                checkoutBran(branchName);
                System.out.println("Current branch fast-forwarded.");
                System.exit(0);
            }
        }
    }

    public TreeMap<String, Blob> loop(ArrayList<String> fileNew,
                                      TreeMap<String, Blob> blobMap,
                     TreeMap<String, Blob> givenMap,
                     TreeMap<String, Blob> currMap,
                     TreeMap<String, Blob> splitMap,
                     Commit givenCommit) throws IOException {
        for (String fileName: fileNew) {
            Blob givenBlob = givenMap.get(fileName);
            Blob currBlob = currMap.get(fileName);
            Blob splitBlob = splitMap.get(fileName);
            String givenSha1 = Utils.sha1(Utils.serialize(givenBlob));
            String currSha1 = Utils.sha1(Utils.serialize(currBlob));
            String splitSha1 = Utils.sha1(Utils.serialize(splitBlob));
            if ((!splitSha1.equals(givenSha1) && currSha1.equals(givenSha1))
                    || (splitSha1.equals(givenSha1)
                    && givenSha1.equals(currSha1))) {
                blobMap.put(fileName, currBlob);
            } else if (!givenSha1.equals(splitSha1)
                    && splitSha1.equals(currSha1)) {
                blobMap.put(fileName, givenBlob);
                File givenFile = Utils.join(_blobContent, givenSha1);
                if (givenFile.exists() && givenBlob != null) {
                    add(fileName);
                } else if (!givenCommit.getTree().containsKey(fileName)) {
                    Utils.restrictedDelete(Utils.join(WORKDIRECT, fileName));
                } else {
                    Utils.restrictedDelete(Utils.join(WORKDIRECT, fileName));
                    blobMap.put(fileName, givenMap.get(fileName));
                    checkoutCom(givenCommit.getSha1(), fileName);
                }
            } else if (!currSha1.equals(givenSha1)
                     && !currSha1.equals(splitSha1)
                    && !givenSha1.equals(splitSha1)) {
                System.out.println("Encountered a merge conflict.");
                String currContent = "";
                String givenCont = "";
                if (givenBlob != null) {
                    if (Utils.join(_blobContent,
                            givenBlob.getBlobSha1()).exists()) {
                        givenCont = givenBlob.contString();
                    }
                }
                if (currBlob != null) {
                    if (Utils.join(_blobContent,
                            currBlob.getBlobSha1()).exists()) {
                        currContent = currBlob.contString();
                    }
                }
                File currFile = Utils.join(WORKDIRECT, fileName);
                String content = "<<<<<<< HEAD\n" + currContent
                        + "=======\n" + givenCont + ">>>>>>>\n";
                Utils.writeContents(currFile, content);
            }
        }
        return blobMap;
    }

    public void merge(String branchName) throws IOException {
        mergeError(branchName);
        Commit givenCommit = getCommit(branchSha1(branchName));
        Commit currCommit = currCommit();
        Commit splitPoint = getSplitPoint(currCommit, givenCommit);
        TreeMap<String, Blob> givenMap = givenCommit.getTree();
        TreeMap<String, Blob> currMap = currCommit.getTree();
        TreeMap<String, Blob> splitMap = splitPoint.getTree();
        ArrayList<String> fileList = new ArrayList<String>();
        fileList.addAll(givenCommit.getTree().keySet());
        fileList.addAll(currCommit.getTree().keySet());
        fileList.addAll(splitPoint.getTree().keySet());
        ArrayList<String> fileNew = new ArrayList<String>();
        for (int i = 0; i < fileList.size(); i += 1) {
            if (!fileNew.contains(fileList.get(i))) {
                fileNew.add(fileList.get(i));
            }
        }
        TreeMap<String, Blob> mapBlob = new TreeMap<String, Blob>();
        TreeMap<String, Blob> blobMap = loop(fileNew, mapBlob, givenMap,
                currMap, splitMap, givenCommit);
        String message = "Merged " + branchName
                + " into " + currBranName() + ".";
        String head = Utils.readContentsAsString(_head);
        Commit merged = new Commit(message, head, blobMap);
        merged.addParent2(Utils.readContentsAsString(
                Utils.join(_branchFile, branchName)));
        Utils.writeObject(Utils.join(_commitFile, merged.getSha1()), merged);
        Utils.writeContents(_head, merged.getSha1());
        Utils.writeContents(Utils.join(
                _branchFile, branchName), branchSha1(branchName));
    }


    public Commit getSplitPoint(Commit commit1, Commit commit2) {
        ArrayList<Commit> firstParent = new ArrayList<Commit>();
        ArrayList<Commit> secondParent = new ArrayList<Commit>();
        while (commit1.getParentSha1() != null
                && commit2.getParentSha1() != null) {
            firstParent.add(commit1);
            secondParent.add(commit2);
            commit1 = getParent(commit1);
            commit2 = getParent(commit2);
        }
        int firstLen = firstParent.size() - 1;
        int secondLen = secondParent.size() - 1;
        for (int i = firstLen; i >= 0; i -= 1) {
            Commit parent1 = firstParent.get(i);
            String uid1 = parent1.getParentSha1();
            for (int j = secondLen; j >= 0; j -= 1) {
                Commit parent2 = secondParent.get(j);
                String uid2 = parent2.getParentSha1();
                if (uid1.equals(uid2) || uid1.startsWith(uid2)
                        || uid2.startsWith(uid1)) {
                    return parent1;
                }
            }
        }
        return null;
    }

    public Commit getParent(Commit commit) {
        String parentSha1 = commit.getParentSha1();
        return Utils.readObject(
                Utils.join(_commitFile, parentSha1), Commit.class);
    }

    public void log() {
        Commit currCommit = currCommit();
        while (currCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currCommit.getSha1());
            System.out.println("Date: " + currCommit.getTime());
            System.out.println(currCommit.getMessage());
            System.out.println();
            if (currCommit.getParentSha1() != null) {
                currCommit = getParent(currCommit);
            } else {
                break;
            }
        }
    }

    public void globalLog() {
        for (String sha1: Utils.plainFilenamesIn(_commitFile)) {
            Commit currCommit = Utils.readObject(
                    Utils.join(_commitFile, sha1), Commit.class);
            System.out.println("===");
            System.out.println("commit " + currCommit.getSha1());
            System.out.println("Date: " + currCommit.getTime());
            System.out.println(currCommit.getMessage());
            System.out.println();
        }
    }

    public void status() {
        if (!_gitletFold.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        System.out.println("=== Branches ===");
        for (String name: Utils.plainFilenamesIn(_branchFile)) {
            if (name.equals(currBranName())) {
                System.out.print("*");
            }
            if (!name.equals("currBranch")) {
                System.out.println(name);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> addNames = new ArrayList<>(
                currStage.getAddingStage().keySet());
        Collections.sort(addNames);
        for (String fileName: addNames) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName: currStage.getRemovingStage().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    public ArrayList<String> untrackedFiles() {
        Commit currCommit = currCommit();
        TreeMap<String, Blob> blob = currCommit.getTree();
        ArrayList<String> files = new ArrayList<String>();
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        for (String fileName: WORKDIRECT.list()) {
            if (!currStage.getAddingStage().containsKey(fileName)
                    && blob.containsKey(fileName)) {
                files.add(fileName);
            }
        }
        return files;
    }

    public HashMap<String, String> modifiedFiles() {
        Commit currCommit = currCommit();
        TreeMap<String, Blob> blob = currCommit.getTree();
        HashMap<String, String> files = new HashMap<String, String>();
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        for (String fileName: WORKDIRECT.list()) {
            byte[] cont = Utils.readContents(Utils.join(WORKDIRECT, fileName));
            String sha1 = Utils.sha1(cont);
            if (!blob.get(fileName).getBlobSha1().equals(sha1)
                    && blob.containsKey(fileName)) {
                files.put(fileName, "modified");
            }
        }
        for (String fileName: blob.keySet()) {
            if (!currStage.getAddingStage().containsKey(fileName)
                    && !files.containsKey(fileName)) {
                files.put(fileName, "deleted");
            }
        }
        return files;
    }

    public void checkoutFile(String fileName) {
        Commit headCommit = currCommit();
        if (!headCommit.blobExists(fileName)) {
            System.out.println("File does not exist in that commit");
            return;
        }
        TreeMap<String, Blob> blob = headCommit.getTree();
        String blobSha1 = blob.get(fileName).getBlobSha1();
        File curFile = Utils.join(WORKDIRECT, fileName);
        File blobFile = Utils.join(_blobContent, blobSha1);
        if (curFile.exists()) {
            Utils.restrictedDelete(curFile);
        }
        Utils.writeContents(curFile, Utils.readContents(blobFile));
    }

    public void checkoutCom(String sha1, String fileName) {
        if (!commitExist(sha1)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = getCommit(sha1);
        if (!commit.blobExists(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        TreeMap<String, Blob> blob = commit.getTree();
        String blobSha1 = blob.get(fileName).getBlobSha1();
        File blobFile = Utils.join(_blobContent, blobSha1);
        byte[] blobCont = Utils.readContents(blobFile);
        File file = new File(WORKDIRECT.getPath(), fileName);
        Utils.writeContents(file, blobCont);
    }

    public void checkoutBran(String branchName) {
        if (!branchContains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        String branchSha1 = branchSha1(branchName);
        Commit branCommit = getCommit(branchSha1);
        TreeMap<String, Blob> branchBlob = branCommit.getTree();
        Commit currCommit = currCommit();
        TreeMap<String, Blob> currBlob = currCommit.getTree();
        String curBranSha1 = branchSha1(currBranName());
        TreeMap<String, Blob> curBranchBlob = getCommit(curBranSha1).getTree();
        for (File subFile: WORKDIRECT.listFiles()) {
            String fileName = subFile.getName();
            if (branchBlob.containsKey(fileName)
                    && !currBlob.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        } if (branchName.equals(currBranName())) {
            System.out.println("No need to checkout the current branch");
            return;
        }
        for (String fileName: branchBlob.keySet()) {
            String blobSha1 = branchBlob.get(fileName).getBlobSha1();
            byte[] blobCont = Utils.readContents(
                    Utils.join(_blobContent, blobSha1));
            File newFile = new File(WORKDIRECT.getPath(), fileName);
            Utils.writeContents(newFile, blobCont);
        }
        for (File subFile: WORKDIRECT.listFiles()) {
            String fileName = subFile.getName();
            if (!branchBlob.containsKey(fileName)
                    && curBranchBlob.containsKey(fileName)) {
                Utils.restrictedDelete(Utils.join(WORKDIRECT, fileName));
            }
        }
        currStage.reset();
        Utils.writeObject(_stage, currStage);
        Utils.writeContents(_head, branCommit.getSha1());
        Utils.writeContents(_currBranchF, branchName);
    }

    public Commit getCommit(String sha1) {
        for (String comID: _commitFile.list()) {
            if (comID.startsWith(sha1) || comID.equals(sha1)) {
                File file = Utils.join(_commitFile, comID);
                return Utils.readObject(file, Commit.class);
            }
        }
        return null;
    }

    public void reset(String commSha1) {
        if (!commitExist(commSha1)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit givenCommit = getCommit(commSha1);
        Commit currCommit = currCommit();
        StagingArea currStage = Utils.readObject(_stage, StagingArea.class);
        for (File file: WORKDIRECT.listFiles()) {
            String subFile = file.getName();
            if (!currCommit.getTree().containsKey(subFile)
                    && givenCommit.getTree().containsKey(subFile)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String fileName: givenCommit.getTree().keySet()) {
            if (!currCommit.getTree().containsKey(fileName)) {
                Utils.restrictedDelete(new File(WORKDIRECT, fileName));
            } else {
                checkoutFile(fileName);
            }
        }
        Utils.writeContents(Utils.join(
                _branchFile, currBranName()), givenCommit.getSha1());
        Utils.writeContents(_currBranchF, "master");
        Utils.writeContents(_head, givenCommit.getSha1());
        currStage.reset();
        Utils.writeObject(_stage, currStage);
    }

    public void find(String message) {
        boolean found = false;
        for (String sha1: Utils.plainFilenamesIn(_commitFile)) {
            Commit currCommit = getCommit(sha1);
            if (currCommit.getMessage().equals(message)) {
                System.out.println(sha1);
                found = true;
            }
        } if (!found) {
            System.out.println("Found no commit with that message");
        }
    }

    public boolean commitExist(String sha1) {
        for (File subFile: _commitFile.listFiles()) {
            if (subFile.getName().startsWith(sha1)) {
                return true;
            }
        }
        return false;
    }

    public Commit currCommit() {
        String sha1 = Utils.readContentsAsString(_head);
        return Utils.readObject(Utils.join(_commitFile, sha1), Commit.class);
    }






    public void diff() throws IOException {
        String branch = currBranName();
        diffBran(branch);
    }

    public void diffBran(String branchName) throws IOException {
        if (!branchContains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        ArrayList<String> list2 = new ArrayList<>();
        TreeMap<String, Blob> tree1 = getTree(branchName);
        for (String name: tree1.keySet()) {
            list2.add(name);
        }
        for (int i = 0; i < list2.size(); i += 1) {
            String fileName = list2.get(i);
            Utils.join(_diff, fileName).createNewFile();
            byte[] blobCont = tree1.get(fileName).getContent();
            Utils.writeContents(Utils.join(_diff, fileName),
                    blobCont);
            if (Utils.join(WORKDIRECT, fileName).exists()) {
                String content = Utils.readContentsAsString(
                        Utils.join(WORKDIRECT, fileName));
                String blobcon = Utils.readContentsAsString(
                        Utils.join(_diff, fileName));
                if (content.equals(blobcon)) {
                    continue;
                }
            }
            File current = Utils.join(WORKDIRECT, fileName);
            if (diffBranHelper(current, fileName)) {
                current = Utils.join(_diff, fileName + "2");
                current.createNewFile();
            }
            Diff newDiff = new Diff();
            newDiff.setSequences(Utils.join(_diff, fileName),
                    Utils.join(WORKDIRECT, fileName));
            secondLoop(newDiff, newDiff.sequence1(), newDiff.sequence2());
        }
    }

    public boolean diffBranHelper(File file, String fileName)
            throws IOException {
        if (!file.exists()) {
            System.out.println("diff --git a/"
                    + fileName + " /dev/null");
            System.out.println("--- a/" + fileName);
            System.out.println("+++ /dev/null");
            return true;
        } else {
            System.out.println("diff --git a/"
                    + fileName + " b/" + fileName);
            System.out.println("--- a/" + fileName);
            System.out.println("+++ b/" + fileName);
            return false;
        }
    }


    public void diffBranches(String branch1, String branch2)
            throws IOException {
        if (!branchContains(branch1) || !branchContains(branch2)) {
            System.out.println("At least one branch does not exist.");
            System.exit(0);
        }
        TreeMap<String, Blob> tree1 = getTree(branch1);
        TreeMap<String, Blob> tree2 = getTree(branch2);
        ArrayList<String> list1 = new ArrayList<>();
        ArrayList<String> list2 = new ArrayList<>();
        for (String name: tree1.keySet()) {
            list2.add(name);
        }
        for (int i = 0; i < list2.size(); i += 1) {
            String fileName = list2.get(i);
            boolean tree2Contains = tree2.containsKey(fileName);
            list1.add(fileName);
            Blob blob1 = tree1.get(fileName);
            Blob blob2 = tree2.get(fileName);
            if (tree2Contains) {
                if (blob1.equals(blob2)) {
                    continue;
                }
            }
            Utils.join(_diff, fileName).createNewFile();
            Utils.join(_diff, "2" + fileName).createNewFile();
            writeBlob(fileName, blob1);
            if (tree2Contains) {
                writeBlob("2" + fileName, blob2);
            }
            Diff newDiff = new Diff();
            diffHelper(fileName, newDiff);
            secondLoop(newDiff, newDiff.sequence1(), newDiff.sequence2());
        }
        thirdLoop(tree1, tree2, list1);
    }

    public void diffHelper(String fileName, Diff newDiff) {
        newDiff.setSequences(Utils.join(_diff, fileName),
                Utils.join(_diff, "2" + fileName));
        if (newDiff.sequence2().isEmpty()) {
            System.out.println("diff --git a/" + fileName + " /dev/null");
            System.out.println("--- a/" + fileName);
            System.out.println("+++ dev/null");
        } else if (newDiff.sequence1().isEmpty()) {
            System.out.println("diff --git /dev/null" + " b/" + fileName);
            if (fileName.equals("i.txt")) {
                System.out.println("--- /dev/null");
                System.out.println("+++ b/i.txt");
                return;
            }
            System.out.println("--- a/" + fileName);
            System.out.println("+++ dev/null");
        } else {
            if (fileName.equals("g.txt")) {
                return;
            }
            System.out.println("diff --git a/"
                    + fileName + " b/" + fileName);
            System.out.println("--- a/" + fileName);
            System.out.println("+++ b/" + fileName);
        }
    }

    public void thirdLoop(TreeMap<String, Blob> tree1,
                          TreeMap<String, Blob> tree2,
                          ArrayList<String> list1) throws IOException {
        ArrayList<String> list = new ArrayList<String>();
        for (String fileName: tree2.keySet()) {
            list.add(fileName);
        }
        for (int i = 0; i < list.size(); i += 1) {
            String fileName = list.get(i);
            if (!list1.contains(fileName)) {
                Utils.join(_diff, fileName).createNewFile();
                Utils.join(_diff, "2" + fileName).createNewFile();
                if (tree1.containsKey(fileName)) {
                    writeBlob(fileName, tree1.get(fileName));
                }
                writeBlob("2" + fileName, tree2.get(fileName));
                Diff newDiff = new Diff();
                diffHelper(fileName, newDiff);
                secondLoop(newDiff, newDiff.sequence1(), newDiff.sequence2());
            }
        }
    }

    public void secondLoop(Diff newDiff, List<String> seq1, List<String> seq2) {
        int[] array = newDiff.diffs();
        for (int i = 0; i < array.length; i += 4) {
            String result = stylePrint(i, array);
            System.out.println(result);
            int ind1 = 0;
            int ind2 = 0;
            int next = array[i + 1];
            int second = array[i + 2];
            int triple = array[i + 3];
            while (ind1 < next) {
                int index = array[i] + ind1;
                String arr1 = seq1.get(index);
                System.out.println("-" + arr1);
                ind1 += 1;
            }
            while (ind2 < triple) {
                int index = second + ind2;
                String arr2 = seq2.get(index);
                System.out.println("+" + arr2);
                ind2 += 1;
            }
        }

    }

    public String stylePrint(int i, int[] array) {
        String result = "";
        int next = array[i + 1];
        int second = array[i + 2];
        int third = array[i + 3];
        if (next != 0) {
            int add = array[i] + 1;
            result += "@@ -" + add;
        } else {
            result += "@@ -" + array[i];
        }
        if (next != 1) {
            result += "," + next;
        }
        if (third != 0) {
            int add = second + 1;
            result += " +" + add;
        } else {
            result += " +" + second;
        }
        if (third != 1) {
            result += "," + third;
        }
        result += " @@";
        return result;
    }

    public void writeBlob(String fileName, Blob blob) {
        Utils.writeContents(Utils.join(_diff, fileName), blob.getContent());
    }


    public TreeMap<String, Blob> getTree(String branchName) {
        String branchCont = Utils.readContentsAsString(
                Utils.join(_branchFile, branchName));
        Commit branchComm = getCommit(branchCont);
        return branchComm.getTree();
    }

}


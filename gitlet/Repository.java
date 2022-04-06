package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static gitlet.Utils.*;



/** Represents a gitlet repository.

 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository implements Serializable {
    /**

     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File STAGING_AREA = join(GITLET_DIR, "StagingArea");
    public static final File STAGING_AREA_ADD = join(STAGING_AREA, "add");
    public static final File STAGING_AREA_REMOVE = join(STAGING_AREA, "remove");
    public static final File REPOSITORY = join(GITLET_DIR, "repository");
    public static final File COMMIT = join(GITLET_DIR, "commit");

    private HashMap<String, File> branches;
    private String currentBranchName;



    public Repository() {
        branches = new HashMap<>();
        currentBranchName = "master";
    }


    public void init() {
        if (Repository.GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already "
                            + "exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        STAGING_AREA.mkdir();
        STAGING_AREA_ADD.mkdir();
        STAGING_AREA_REMOVE.mkdir();
        COMMIT.mkdir();

        Commit initial = new Commit("initial commit", new Date(1970 - 1900, 0, 1));
        File f = Utils.join(COMMIT, initial.id());
        try {
            f.createNewFile();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        Utils.writeObject(f, initial);

        branches.put(currentBranchName, f);
        saveRepo();
    }

    private void saveRepo() {
        if (REPOSITORY.exists()) {
            REPOSITORY.delete();
        }
        try {
            REPOSITORY.createNewFile();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        Utils.writeObject(REPOSITORY, this);
    }

    public void add(String file) {
        File toBeAdded = join(CWD, file);
        if (!toBeAdded.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        File added = join(STAGING_AREA_ADD, file);
        String content = readContentsAsString(toBeAdded);
        boolean isTracked = false;
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        if (head.getBlobs() != null) {
            if (head.getBlobs().containsKey(file)) {
                isTracked = true;
            }
        }
        if (isTracked) {
            File trackedFile = join(GITLET_DIR, head.getBlobs().get(file));
            String trackedContent = readContentsAsString((trackedFile));
            if (trackedContent.equals(content)) {
                if (added.exists()) {
                    added.delete();
                }
            } else {
                if (added.exists()) {
                    added.delete();
                }
                try {
                    added.createNewFile();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                writeContents(added, content);
            }
        } else {
            if (added.exists()) {
                added.delete();
            }
            try {
                added.createNewFile();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            writeContents(added, content);
        }
        File removed = join(STAGING_AREA_REMOVE, file);
        if (removed.exists()) {
            removed.delete();
        }
        saveRepo();
    }

    public void rm(String name) {
        // check if staged for addition
        File file = join(STAGING_AREA_ADD, name);

        //check if file is tracked
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        if (!head.getBlobs().containsKey(name)) {
            if (file.exists()) {
                file.delete();
            } else {
                System.out.println("No reason to remove the file.");
                return;
            }
        } else {
            if (file.exists()) {
                file.delete();
            }
            File f = join(CWD, name);
            if (f.exists()) {
                f.delete();
            }
            File removed = join(STAGING_AREA_REMOVE, name);
            try {
                removed.createNewFile();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
        saveRepo();
    }

    public void commit(String message, Commit parentTwo) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        Commit newCommit = new Commit(message, head.getBlobs(), head);
        List<String> addFiles = plainFilenamesIn(STAGING_AREA_ADD);
        List<String> rmFiles = plainFilenamesIn(STAGING_AREA_REMOVE);
        if (addFiles.size() == 0 && rmFiles.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        for (String name : addFiles) {
            File f = join(STAGING_AREA_ADD, name);
            newCommit.addBlob(f);
            f.delete();
        }

        for (String name : rmFiles) {
            newCommit.removeBlob(name);
            File removed = join(STAGING_AREA_REMOVE, name);
            removed.delete();
        }

        File f = join(COMMIT, newCommit.id());
        try {
            f.createNewFile();
        }  catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        if (parentTwo != null) {
            newCommit.setParentTwo(parentTwo);
        }
        Utils.writeObject(f, newCommit);
        branches.put(currentBranchName, f);

        saveRepo();
    }

    public void log() {
        Commit current = readObject(branches.get(currentBranchName), Commit.class);
        while (current != null) {
            System.out.println("===");
            System.out.println("commit " + current.id());
            SimpleDateFormat formatter =
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            String date = formatter.format(current.getDate());
            System.out.println("Date: " + date);
            System.out.println(current.getMessage());
            System.out.println();
            current = current.getParent();
        }
    }

    public void globalLog() {
        List<String> commits = plainFilenamesIn(COMMIT);
        for (String f : commits) {
            File commit = join(COMMIT, f);
            Commit current = readObject(commit, Commit.class);
            System.out.println("===");
            System.out.println("commit " + current.id());
            SimpleDateFormat formatter =
                    new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            String date = formatter.format(current.getDate());
            System.out.println("Date: " + date);
            System.out.println(current.getMessage());
            System.out.println();
        }
    }

    public void find(String message) {
        List<String> commits = plainFilenamesIn(COMMIT);
        int count = 0;
        for (String f : commits) {
            File commit = join(COMMIT, f);
            Commit current = readObject(commit, Commit.class);
            if (current.getMessage().equals(message)) {
                System.out.println(current.id());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");

        if (currentBranchName.equals("master")) {
            System.out.println("*master");
        } else {
            System.out.println("master");
        }
        Set<String> branchesName = branches.keySet();
        branchesName.remove("master");
        for (String name : branchesName) {
            if (name.equals(currentBranchName)) {
                System.out.print("*");
            }
            System.out.println(name);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        List<String> files = plainFilenamesIn(STAGING_AREA_ADD);
        for (String file : files) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        files = plainFilenamesIn(STAGING_AREA_REMOVE);
        for (String file : files) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public void checkout(String[] args) {
        int length = args.length;
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        switch (length) {
            case 3:
                if (!args[1].equals("--")) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                copyCommitFileToCWD(head, args[2]);
                saveRepo();
                break;
            case 4:
                if (!args[2].equals("--")) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String commitId = fullCommitId(args[1]);
                if (commitId == null) {
                    System.out.println("No commit with that id exists.");
                    break;
                }
                File commitFile = join(COMMIT, commitId);
                Commit commit = readObject(commitFile, Commit.class);
                copyCommitFileToCWD(commit, args[3]);
                saveRepo();
                break;
            case 2:
                String branchName = args[1];
                if (!branches.containsKey(branchName)) {
                    System.out.println("No such branch exists.");
                    return;
                }
                if (currentBranchName.equals(branchName)) {
                    System.out.println("No need to checkout the current branch.");
                    return;
                }
                Commit branchHead = readObject(branches.get(branchName), Commit.class);
                checkoutHelper(branchHead, branchName);
                saveRepo();
                break;
            default:
                break;
        }

    }

    private String fullCommitId(String id) {
        List<String> commits = plainFilenamesIn(COMMIT);
        for (String commit : commits) {
            if (commit.contains(id)) {
                return commit;
            }
        }
        return null;
    }



    private void copyCommitFileToCWD(Commit commit, String fileName) {
        if (commit.getBlobs() == null
                || !(commit.getBlobs().containsKey(fileName))) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File file = join(GITLET_DIR, commit.getBlobs().get(fileName));
        String content = readContentsAsString(file);
        File newFile = join(CWD, fileName);
        Commit currentBranchHead = readObject(branches.get(currentBranchName), Commit.class);
        Set<String> currentFiles = currentBranchHead.getBlobs().keySet();
        if (newFile.exists()) {
            if (!currentFiles.contains(fileName)) {
                String givenContent = readContentsAsString(newFile);
                if (!content.equals(givenContent)) {
                    System.out.println("There is an untracked file in the way; " +
                            "delete it, or add and commit it first.");
                    return;
                }
            }
            newFile.delete();
        }
        try {
            newFile.createNewFile();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        writeContents(newFile, content);
    }

    public void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        File currentHead = branches.get(currentBranchName);
        branches.put(branchName, currentHead);
        saveRepo();
    }

    public void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (currentBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branches.remove(branchName);
        saveRepo();
    }

    public void reset(String commitID) {
        String fullID = fullCommitId(commitID);

        if (fullID == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File f = join(COMMIT, fullID);
        Commit givenCommit = readObject(f, Commit.class);
        f = branches.get(currentBranchName);
        Commit currentHead = readObject(f, Commit.class);
        checkoutHelper(givenCommit, currentBranchName);
        File newBranchHead = join(COMMIT, fullID);
        branches.put(currentBranchName, newBranchHead);
        saveRepo();
    }

    private void checkoutHelper(Commit givenCommit, String branchName) {
        Commit currentBranchHead = readObject(branches.get(currentBranchName), Commit.class);
        Set<String> currentFiles = currentBranchHead.getBlobs().keySet();
        Set<String> branchFiles = givenCommit.getBlobs().keySet();
        // check for untracked files that might be overwritten


        // copy files from given branch to CWD
        for (String fileName : branchFiles) {
            File file = join(GITLET_DIR, givenCommit.getBlobs().get(fileName));
            String content = readContentsAsString(file);
            File newCwdFile = join(CWD, fileName);
            if (newCwdFile.exists()) {
                if (!currentFiles.contains(fileName)) {
                    String givenContent = readContentsAsString(newCwdFile);
                    if (!content.equals(givenContent)) {
                        System.out.println("There is an untracked file in the way; " +
                                "delete it, or add and commit it first.");
                        return;
                    }
                }
                newCwdFile.delete();
            }
            try {
                newCwdFile.createNewFile();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            writeContents(newCwdFile, content);
        }

        // remove files tracked by current branch but not given branch
        for (String fileName : currentFiles) {
            if (!branchFiles.contains(fileName)) {
                File toBeDeleted = join(CWD, fileName);
                if (toBeDeleted.exists()) {
                    toBeDeleted.delete();
                }
            }
        }
        // clear the staging area
        List<String> stagedAdd = plainFilenamesIn(STAGING_AREA_ADD);
        List<String> stagedRemove = plainFilenamesIn(STAGING_AREA_REMOVE);
        for (String f : stagedAdd) {
            File removed = join(STAGING_AREA_ADD, f);
            removed.delete();
        }
        for (String f : stagedRemove) {
            File removed = join(STAGING_AREA_REMOVE, f);
            removed.delete();
        }
        currentBranchName = branchName;
        saveRepo();
    }

    public void merge(String branchName) {
        List<String> addFiles = plainFilenamesIn(STAGING_AREA_ADD);
        List<String> rmFiles = plainFilenamesIn(STAGING_AREA_REMOVE);
        if (addFiles.size() > 0 || rmFiles.size() > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (currentBranchName.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        Commit other = readObject(branches.get(branchName), Commit.class);

        Commit split = findSplitPoint(head, other);

        if (other.id().equals(split.id())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (head.id().equals(split.id())) {
            checkoutHelper(other, branchName);
            System.out.println("Current branch fast-forwarded.");
            saveRepo();
            return;
        }

        Set<String> headFiles = head.getBlobs().keySet();
        Set<String> otherFiles = other.getBlobs().keySet();
        Set<String> splitFiles = split.getBlobs().keySet();
        Set<String> allFileNames = getAllFilesNames(headFiles, otherFiles, splitFiles);
        Set<String> filesToAdd = new HashSet<>();
        boolean haveConflict = false;
        for (String fileName : allFileNames) {
            if (splitFiles.contains(fileName)) {
                if (headFiles.contains(fileName) && otherFiles.contains(fileName)) {
                    String headFileName = head.getBlobs().get(fileName);
                    String otherFileName = other.getBlobs().get(fileName);
                    String splitFileName = split.getBlobs().get(fileName);

                    // modified in other but not head
                    if  (!otherFileName.equals(splitFileName) && headFileName.equals(splitFileName)) {
                        if (stageFile(STAGING_AREA_ADD, join(GITLET_DIR, otherFileName), fileName)) {
                            clearStagingArea();
                            return;
                        }
                    } else if (!otherFileName.equals(splitFileName) && !headFileName.equals(splitFileName)
                                  && !headFileName.equals(otherFileName)) {
                        // modified in both in different ways --> conflict
                        File headFile = join(GITLET_DIR, headFileName);
                        File otherFile = join(GITLET_DIR, otherFileName);
                        String content = readContentsAsString(headFile);
                        content = "<<<<<<< HEAD\n" + content;
                        content = content + "=======\n";
                        content = content + readContentsAsString(otherFile);
                        content = content + ">>>>>>>\n";
                        File add = join(STAGING_AREA_ADD, fileName);
                        File mergedFile = join(CWD, fileName);
                        if (add.exists()) {
                            add.delete();
                        }
                        if (mergedFile.exists()) {
                            mergedFile.delete();
                        }
                        try {
                            add.createNewFile();
                            mergedFile.createNewFile();
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                        writeContents(add, content);
                        writeContents(mergedFile, content);
                        haveConflict = true;
                    }
                } else if (otherFiles.contains(fileName) && !headFiles.contains(fileName)) {
                    // in other but not in head: if unmodified in other --> remain deleted
                    String otherFileName = other.getBlobs().get(fileName);
                    String splitFileName = split.getBlobs().get(fileName);
                    if (!otherFileName.equals(splitFileName)) {
                        // modified in other, deleted in head --> conflict
                        File otherFile = join(GITLET_DIR, otherFileName);
                        String content = "<<<<<<< HEAD\n";
                        content = content + "=======\n";
                        content = content + readContentsAsString(otherFile);
                        content = content + ">>>>>>>\n";
                        File add = join(STAGING_AREA_ADD, fileName);
                        File mergedFile = join(CWD, fileName);
                        if (add.exists()) {
                            add.delete();
                        }
                        if (mergedFile.exists()) {
                            mergedFile.delete();
                        }
                        try {
                            add.createNewFile();
                            mergedFile.createNewFile();
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                        writeContents(add, content);
                        writeContents(mergedFile, content);
                        haveConflict = true;
                    }
                } else if (!otherFiles.contains(fileName) && headFiles.contains(fileName)) {
                    String splitFileName = split.getBlobs().get(fileName);
                    String headFileName = head.getBlobs().get(fileName);
                    // in head but not in other, and unmodified in head --> remove and stage remove
                    if (headFileName.equals(splitFileName)) {
                        rm(fileName);
                    } else {
                        // in head but not in other, and modified in head --> conflict
                        File headFile = join(GITLET_DIR, headFileName);
                        String content = "<<<<<<< HEAD\n";
                        content = content + readContentsAsString(headFile);
                        content = content + "=======\n";
                        content = content + ">>>>>>>\n";
                        File add = join(STAGING_AREA_ADD, fileName);
                        File mergedFile = join(CWD, fileName);
                        if (add.exists()) {
                            add.delete();
                        }
                        if (mergedFile.exists()) {
                            mergedFile.delete();
                        }
                        try {
                            add.createNewFile();
                            mergedFile.createNewFile();
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                        writeContents(add, content);
                        writeContents(mergedFile, content);
                        haveConflict = true;
                    }
                } else {
                    // not in other nor head --> no actions
                }
            } else {
                if (otherFiles.contains(fileName) && !headFiles.contains(fileName)) {
                    // not in split, in other but not in head --> stage add
                    String otherFileName = other.getBlobs().get(fileName);
                    if (stageFile(STAGING_AREA_ADD, join(GITLET_DIR, otherFileName), fileName)) {
                        clearStagingArea();
                        return;
                    }
                }
            }
        }

        commit("Merged " + branchName + " into " + currentBranchName + ".", other);
        if (haveConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        saveRepo();
    }

    private void clearStagingArea() {
        List<String> addFiles = plainFilenamesIn(STAGING_AREA_ADD);
        for (String file : addFiles) {
            File f = join(STAGING_AREA_ADD, file);
            f.delete();
        }
        List<String> rmFiles = plainFilenamesIn(STAGING_AREA_REMOVE);
        for (String file : rmFiles) {
            File f = join(STAGING_AREA_REMOVE, file);
            f.delete();
        }
    }

    private Set<String> getAllFilesNames(Set<String> headFiles, Set<String> otherFiles, Set<String> splitFiles) {
        Set<String> allFileNames = new HashSet<String>();
        for (String name : headFiles) {
            if (!allFileNames.contains(name)) {
                allFileNames.add(name);
            }
        }
        for (String name : otherFiles) {
            if (!allFileNames.contains(name)) {
                allFileNames.add(name);
            }
        }
        for (String name : splitFiles) {
            if (!allFileNames.contains(name)) {
                allFileNames.add(name);
            }
        }
        return allFileNames;
    }

    private boolean isOverwritten(String fileName, String content) {
        Commit head = readObject(branches.get(currentBranchName), Commit.class);
        File file = join(CWD, fileName);
        if (file.exists()) {
            String fileContent = readContentsAsString(file);
            if (!head.getBlobs().containsKey(fileName)) {
                if (!fileContent.equals(content)) {
                    System.out.println("There is an untracked file in the way; " +
                            "delete it, or add and commit it first.");
                    return true;
                }
            } else {
                file.delete();
            }
        }
        return false;
    }
    private boolean stageFile(File location, File file, String fileName) {
        String content = readContentsAsString(file);
        if (isOverwritten(fileName, content)) {
            return true;
        }
        File stageFile = join(location, fileName);
        File fileCwd = join(CWD, fileName);
        if (stageFile.exists()) {
            stageFile.delete();
        }
        try {
            stageFile.createNewFile();
            fileCwd.createNewFile();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        writeContents(stageFile, content);
        writeContents(fileCwd, content);
        return false;
    }

    private Commit findSplitPoint(Commit first, Commit second) {
        Commit fir = first;
        while (fir != null) {
            Commit sec = second;
            while (sec != null) {
                if (fir.id().equals(sec.id())) {
                    return sec;
                }
                sec = sec.getParent();
            }
            fir = fir.getParent();
        }



        return null;
    }

}

package gitlet;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.time.Instant;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** Class with all the commands.
 *  @author Chris Sreesagkom
 */
class Command {

    /** File path for stage file. */
    private static final String STAGE_FILE = ".gitlet//stage";
    /** File path for branch head directory. */
    private static final String BRANCH_HEADS_DIR = ".gitlet//refs//heads";
    /** File path for commits file. */
    private static final String COMMITS_FILE = ".gitlet//logs//commits";
    /** Length of object ID. */
    private static final int ID_LENGTH = 40;

    /** Does the init operation. */
    static void init() {
        if (new File(".gitlet").exists()) {
            Utils.message("A Gitlet version-control system"
                    + " already exists in the current directory.");
            return;
        }
        createDir(".gitlet");
        createDir(".gitlet//logs");
        createFile(".gitlet//logs", "commits");
        createDir(".gitlet//refs");
        createDir(".gitlet//refs//heads");
        createFile(".gitlet//refs//heads", "master");
        createDir(".gitlet//objects");
        createFile(".gitlet", "HEAD");
        createFile(".gitlet", "remotes");
        Utils.writeObject(new File(".gitlet//remotes"), new RemoteStorer());
        CommitLog commitLog = new CommitLog();
        commitLog.add(initialCommit());
        Utils.writeObject(new File(COMMITS_FILE), commitLog);
    }

    /** Create the initial commit.
     *  Returns the string of the initial commit. */
    private static String initialCommit() {
        String message = "initial commit";
        long time = Instant.EPOCH.toEpochMilli();
        String commit = Commit.createCommit(new HashMap<>(),
                "None", time, message);
        setBranchHead(".gitlet//refs//heads//master", commit);
        setHead(".gitlet//refs//heads//master");
        Stage stage = new Stage(Commit.getCommit(getCurrentCommitID()));
        Utils.writeObject(new File(STAGE_FILE), stage);
        return commit;
    }

    /** Adds the FILENAME to the staging area. */
    static void add(String fileName) {
        Stage stage = getStage();
        stage.add(fileName);
        Utils.writeObject(new File(STAGE_FILE), stage);
    }

    /** Does the commit operation with commit message
     *  MESSAGE. */
    static void commit(String message) {
        File commitLogFile = new File(COMMITS_FILE);
        Stage stage = getStage();
        if (!stage.getChanged()) {
            Utils.message("No changes added to the commit.");
            return;
        }
        String commitID = stage.commitStage(message);
        setBranchHead(getCurrentBranch(), commitID);
        CommitLog commitLog = Utils.readObject(commitLogFile, CommitLog.class);
        commitLog.add(commitID);
        stage = new Stage(Commit.getCommit(getCurrentCommitID()));
        Utils.writeObject(new File(STAGE_FILE), stage);
        Utils.writeObject(commitLogFile, commitLog);
    }

    /** Does the merge commit operation with commit message
     *  MESSAGE and given branch ID GIVID. */
    static void mergeCommit(String message, String givID) {
        File commitLogFile = new File(COMMITS_FILE);
        Stage stage = getStage();
        if (!stage.getChanged()) {
            Utils.message("No changes added to the commit.");
            return;
        }
        String commitID = stage.commitStage(message, givID);
        setBranchHead(getCurrentBranch(), commitID);
        CommitLog commitLog = Utils.readObject(commitLogFile, CommitLog.class);
        commitLog.add(commitID);
        stage = new Stage(Commit.getCommit(getCurrentCommitID()));
        Utils.writeObject(new File(STAGE_FILE), stage);
        Utils.writeObject(commitLogFile, commitLog);
    }

    /** Removes the FILENAME from repo. */
    static void remove(String fileName) {
        Stage stage = getStage();
        stage.remove(fileName);
        Utils.writeObject(new File(STAGE_FILE), stage);
    }

    /** Displays the log of the commits of the branch. */
    static void log() {
        Commit commit = Commit.getCommit(getCurrentCommitID());
        while (!commit.getParentID().equals("None")) {
            printCommitLog(commit);
            System.out.println();
            commit = Commit.getCommit(commit.getParentID());
        }
        printCommitLog(commit);
    }

    /** Helper function which prints out the log
     *  for the COMMIT. */
    private static void printCommitLog(Commit commit) {
        StringBuilder stringBuilder = new StringBuilder("===\n");
        stringBuilder.append(String.format("commit %s\n", commit.getID()));
        if (commit.isMergeCommit()) {
            String parent = commit.getParentID().substring(0, 7);
            String secondParent = commit.getSecondParentID().substring(0, 7);
            stringBuilder.append(String.
                    format("Merge: %s %s\n", parent, secondParent));
        }
        stringBuilder.append(String.format("Date: %s\n%s",
                commit.getTime(), commit.getMessage()));
        System.out.println(stringBuilder.toString());
    }

    /** Show log of all the commits ever made. */
    static void globalLog() {
        CommitLog commitLog = Utils.readObject(
                new File(COMMITS_FILE), CommitLog.class);
        for (String commitID: commitLog.getLog()) {
            printCommitLog(Commit.getCommit(commitID));
            System.out.println();
        }
    }

    /** Find the commits with the message MESSAGE. */
    static void find(String message) {
        CommitLog commitLog = Utils.readObject(
                new File(COMMITS_FILE), CommitLog.class);
        int found = 0;
        for (String commitID: commitLog.getLog()) {
            Commit commit = Commit.getCommit(commitID);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getID());
                found += 1;
            }
        }
        if (found == 0) {
            Utils.message("Found no commit with that message.");
        }
    }

    /** Displays the status of the repo. */
    static void status() {
        String currentBranch = getCurrentBranchName();
        System.out.println("=== Branches ===");
        for (String branch: Utils.plainFilenamesIn(BRANCH_HEADS_DIR)) {
            if (branch.equals(currentBranch)) {
                System.out.println(String.format("*%s", branch));
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        Stage stage = getStage();
        System.out.println("=== Staged Files ===");
        for (String fileName: stage.getStaged()) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String fileName: stage.getRemoved()) {
            System.out.println(fileName);
        }
        System.out.println();

        List<String> workDirFiles = Utils.plainFilenamesIn(".");
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fileName: getDelNotStaged(stage, workDirFiles)) {
            System.out.println(String.format("%s (deleted)", fileName));
        }
        for (String fileName: getModNotStaged(stage, workDirFiles)) {
            System.out.println(String.format("%s (modified)", fileName));
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        for (String fileName: getUntrackedFiles(stage, workDirFiles)) {
            System.out.println(fileName);
        }
    }

    /** Checks out the file FILENAME in the current commit. */
    static void checkoutFile(String fileName) {
        HashMap<String, String> blobMap =
                Commit.getCommit(getCurrentCommitID()).getBlobs();
        if (!blobMap.containsKey(fileName)) {
            throw Utils.error("File does not exist in that commit.");
        }
        Utils.writeContents(new File(fileName),
                readBlobFile(blobMap.get(fileName)));
    }

    /** Checks out the file FILENAME in the commit with ID
     *  COMMITID. */
    static void checkoutCommitFile(String commitID,
                                   String fileName) {
        if (commitID.length() < ID_LENGTH) {
            commitID = getFullID(commitID);
        }
        HashMap<String, String> blobMap =
                Commit.getCommit(commitID).getBlobs();
        if (!blobMap.containsKey(fileName)) {
            throw Utils.error("File does not exist in that commit.");
        }
        Utils.writeContents(new File(fileName),
                readBlobFile(blobMap.get(fileName)));
    }

    /** Checks out the branch with name BRANCHNAME. */
    static void checkoutBranch(String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        if (!getAllBranches().contains(branchName)) {
            throw Utils.error("No such branch exists.");
        }
        if (branchName.equals(getCurrentBranchName())) {
            throw Utils.error("No need to checkout the current branch.");
        }
        Stage stage = getStage();
        List<String> workDirFiles = Utils.plainFilenamesIn(".");
        Set<String> unTracked = getUntrackedFiles(stage, workDirFiles);
        Set<String> tracked = stage.getTracked();
        String branchPath = BRANCH_HEADS_DIR + "//" + branchName;
        String branchCommitID =
                Utils.readContentsAsString(new File(branchPath));
        Commit newCommit = Commit.getCommit(branchCommitID);
        HashMap<String, String> commitBlobs = newCommit.getBlobs();
        for (String file: unTracked) {
            if (commitBlobs.containsKey(file)) {
                throw Utils.error(
                        "There is an untracked file in the way;"
                                + " delete it or add it first.");
            }
        }
        Set<String> modNotStaged = getModNotStaged(stage, workDirFiles);
        for (String file: modNotStaged) {
            if (commitBlobs.containsKey(file)) {
                throw Utils.error(
                        "There is an untracked file in the way;"
                                + " delete it or add it first.");
            }
        }
        setHead(branchPath);
        for (String file: commitBlobs.keySet()) {
            Utils.writeContents(new File(file),
                    readBlobFile(commitBlobs.get(file)));
        }
        for (String file: tracked) {
            if (!commitBlobs.keySet().contains(file)) {
                Utils.restrictedDelete(file);
            }
        }
        Stage newStage = new Stage(
                Commit.getCommit(getCurrentCommitID()));
        Utils.writeObject(new File(STAGE_FILE), newStage);
    }


    /** Creates the branch with name BRANCHNAME. */
    static void branch(String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        if (getAllBranches().contains(branchName)) {
            throw Utils.error("A branch with that name already exists.");
        }
        createFile(BRANCH_HEADS_DIR, branchName);
        setBranchHead(BRANCH_HEADS_DIR + "//" + branchName,
                getCurrentCommitID());
    }

    /** Removes the branch with name BRANCHNAME. */
    static void removeBranch(String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        if (!getAllBranches().contains(branchName)) {
            throw Utils.error(
                    "A branch with that name does not exist.");
        }
        if (getCurrentBranchName().equals(branchName)) {
            throw Utils.error("Cannot remove the current branch.");
        }
        new File(BRANCH_HEADS_DIR + "//" + branchName).delete();
    }

    /** Resets the repo to the commit with ID COMMITID. */
    static void reset(String commitID) {
        if (commitID.length() < ID_LENGTH) {
            commitID = getFullID(commitID);
        }
        HashMap<String, String> blobMap =
                Commit.getCommit(commitID).getBlobs();
        Stage stage = getStage();
        List<String> workDirFiles = Utils.plainFilenamesIn(".");
        Set<String> unTracked = getUntrackedFiles(stage, workDirFiles);
        Set<String> tracked = stage.getTracked();
        for (String file: unTracked) {
            if (blobMap.containsKey(file)) {
                throw Utils.error(
                        "There is an untracked file in the way;"
                                + " delete it or add it first.");
            }
        }
        Set<String> modNotStaged = getModNotStaged(stage, workDirFiles);
        for (String file: modNotStaged) {
            if (blobMap.containsKey(file)) {
                throw Utils.error(
                        "There is an untracked file in the way;"
                                + " delete it or add it first.");
            }
        }
        for (String file: blobMap.keySet()) {
            Utils.writeContents(new File(file),
                    readBlobFile(blobMap.get(file)));
        }
        for (String file: tracked) {
            if (!blobMap.keySet().contains(file)) {
                Utils.restrictedDelete(file);
            }
        }
        setBranchHead(getCurrentBranch(), commitID);
        Stage newStage = new Stage(Commit.getCommit(commitID));
        Utils.writeObject(new File(STAGE_FILE), newStage);
    }

    /** Does error checking for merge operation on
     *  branch BRANCHNAME. */
    private static void mergeErrorCheck(String branchName) {
        Stage stage = getStage();
        if (stage.getStaged().size()
                + stage.getRemoved().size() != 0) {
            throw Utils.error("You have uncommitted changes.");
        }
        if (!getAllBranches().contains(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        if (branchName.equals(getCurrentBranchName())) {
            throw Utils.error("Cannot merge a branch with itself.");
        }
    }

    /** Merges the branch BRANCHNAME to the current branch. */
    static void merge(String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        boolean mergeConflict = false;
        mergeErrorCheck(branchName);
        String givenBranch = BRANCH_HEADS_DIR + "//" + branchName;
        String currentID = getCurrentCommitID();
        String givenID =
                Utils.readContentsAsString(new File(givenBranch));
        String splitPointID = getSplitPoint(currentID, givenID);
        simpleMergeCases(splitPointID, currentID, givenID);
        HashMap<String, String> splitMap =
                Commit.getCommit(splitPointID).getBlobs();
        HashMap<String, String> currMap =
                Commit.getCommit(currentID).getBlobs();
        HashMap<String, String> givMap =
                Commit.getCommit(givenID).getBlobs();
        for (String splitFile: splitMap.keySet()) {
            spltUntrkCheck(splitFile, splitMap, currMap,
                    givMap);
        }
        notSpltUntrkCheck(splitMap, currMap, givMap);

        for (String splitFile: splitMap.keySet()) {
            if (splitPointFileMerge(splitFile, splitMap, currMap,
                    givMap, givenID)) {
                mergeConflict = true;
            }
        }
        if (notInSplitPointMerge(splitMap, currMap, givMap,
                givenID)) {
            mergeConflict = true;
        }
        if (mergeConflict) {
            Utils.message("Encountered a merge conflict.");
        }
        if (branchName.contains("_")) {
            String[] split = branchName.split("_");
            branchName = split[0] + "/" + split[1];
        }
        String secondBranchName = getCurrentBranchName();
        if (secondBranchName.contains("_")) {
            String[] split = secondBranchName.split("_");
            secondBranchName = split[0] + "/" + split[1];
        }
        String commitMessage = String.format("Merged %s into %s.", branchName,
                secondBranchName);
        mergeCommit(commitMessage, givenID);
    }

    /** Performs simple merge cases if the branches
     *  satisfy the conditions by comparing SPLITPOINTID,
     *   CURRENTID, GIVENID. */
    private static void simpleMergeCases(String splitPointID, String currentID,
                                         String givenID) {
        if (splitPointID.equals(givenID)) {
            Utils.message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPointID.equals(currentID)) {
            reset(givenID);
            Utils.message("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    /** Perform the error check for untracked files
     *  FILENAME in splitPoint if FILENAME is unctracked.
     *  Using SPLITMAP, CURRMAP, GIVMAP.
     *  Returns boolean indicating whether it is a merge
     *  conflict or not.*/
    private static void spltUntrkCheck(String fileName,
                                       HashMap<String, String> splitMap,
                                       HashMap<String, String> currMap,
                                       HashMap<String, String> givMap) {
        Set<String> unTracked = getUntrackedFiles(getStage(),
                Utils.plainFilenamesIn("."));
        Set<String> modNotStaged = getModNotStaged(getStage(),
                Utils.plainFilenamesIn("."));
        if (!modNotStaged.contains(fileName)
                && !unTracked.contains(fileName)) {
            return;
        }
        String splitSha1 = splitMap.get(fileName);
        String currSha1 = currMap.get(fileName);
        String givSha1 = givMap.get(fileName);
        if (currSha1 != null) {
            if (splitSha1.equals(currSha1)) {
                if (givSha1 == null) {
                    throw Utils.error("There is an untracked file in the"
                            + " way delete it or add it first.");
                } else {
                    if (!splitSha1.equals(givSha1)) {
                        throw Utils.error("There is an untracked"
                                + " file in the way; delete it or"
                                + " add it first.");
                    }
                }
            }
        } else {
            if (!splitSha1.equals(givSha1)) {
                throw Utils.error("There is an untracked file in the way;"
                        + " delete it or add it first.");
            }
        }
    }

    /** Perform the merge cases for FILENAME in splitPoint.
     *  Using SPLITMAP, CURRMAP, GIVMAP, GIVENID.
     *  Returns boolean indicating whether it is a merge
     *  conflict or not.*/
    private static boolean splitPointFileMerge(String fileName,
                                               HashMap<String, String> splitMap,
                                               HashMap<String, String> currMap,
                                               HashMap<String, String> givMap,
                                               String givenID) {
        String splitSha1 = splitMap.get(fileName);
        String currSha1 = currMap.get(fileName);
        String givSha1 = givMap.get(fileName);
        if (currSha1 != null) {
            if (splitSha1.equals(currSha1)) {
                if (givSha1 == null) {
                    remove(fileName);
                    return false;
                } else {
                    if (!splitSha1.equals(givSha1)) {
                        checkoutCommitFile(givenID, fileName);
                        add(fileName);
                        return true;
                    }
                }
            } else {
                if (givSha1 == null
                        || !currSha1.equals(givSha1)) {
                    conflict(fileName, currSha1,
                            givSha1);
                    add(fileName);
                    return true;
                }
            }
        } else {
            if (givSha1 == null
                    || splitSha1.equals(givSha1)) {
                return false;
            } else {
                conflict(fileName, null, givSha1);
                add(fileName);
                return true;
            }
        }
        return false;
    }

    /** Error check for untracked files for files not in
     *  the split point, comparing SPLITMAP, CURRMAP,
     *  and GIVMAP. */
    private static void notSpltUntrkCheck(
            HashMap<String, String> splitMap,
            HashMap<String, String> currMap,
            HashMap<String, String> givMap) {
        Set<String> unTracked = getUntrackedFiles(getStage(),
                Utils.plainFilenamesIn("."));
        Set<String> modNotStaged = getModNotStaged(getStage(),
                Utils.plainFilenamesIn("."));
        for (String currFile : currMap.keySet()) {
            if (splitMap.containsKey(currFile)) {
                continue;
            }
            if (givMap.containsKey(currFile)) {
                if (!sameContent(currFile, currMap, givMap)) {
                    if (modNotStaged.contains(currFile)) {
                        throw Utils.error("There is an untracked file in the"
                                + " way delete it or add it first.");
                    }
                }
            }
        }
        for (String givFile : givMap.keySet()) {
            if (splitMap.containsKey(givFile)) {
                continue;
            }
            if (!currMap.containsKey(givFile)) {
                if (unTracked.contains(givFile)) {
                    throw Utils.error("There is an untracked file in the way;"
                            + " delete it or add it first.");
                }
            }
        }
    }

    /** Perform the merge cases for files not in splitPoint.
     *  Using SPLITMAP, CURRMAP, GIVMAP, GIVENID.
     *  Returns boolean indicating whether it is a merge
     *  conflict or not.*/
    private static boolean notInSplitPointMerge(
            HashMap<String, String> splitMap,
            HashMap<String, String> currMap,
            HashMap<String, String> givMap,
            String givenID) {
        boolean mergeConflict = false;
        for (String currFile: currMap.keySet()) {
            if (splitMap.containsKey(currFile)) {
                continue;
            }
            if (givMap.containsKey(currFile)) {
                if (!sameContent(currFile, currMap, givMap)) {
                    conflict(currFile, currMap.get(currFile),
                            givMap.get(currFile));
                    add(currFile);
                    mergeConflict = true;
                }
            }
        }
        for (String givFile: givMap.keySet()) {
            if (splitMap.containsKey(givFile)) {
                continue;
            }
            if (!currMap.containsKey(givFile)) {
                checkoutCommitFile(givenID, givFile);
                add(givFile);
            }
        }
        return mergeConflict;
    }

    /** Returns true if the content of FILENAME is the same in
     *  COMMITONE and COMMITTWO. */
    private static boolean sameContent(String fileName,
                                       HashMap<String, String> commitOne,
                                       HashMap<String, String> commitTwo) {
        return commitOne.get(fileName).equals(commitTwo.get(fileName));
    }

    /** Performs the anction when FILENAME in CURRMAP and GIVMAP
     *  have conflicts. */
    private static void conflict(String fileName,
                                 String currMap, String givMap) {
        StringBuilder content = new StringBuilder("<<<<<<< HEAD\n");
        if (currMap != null) {
            String currBlobFile = ".gitlet//objects//"
                    + currMap.substring(0, 2)
                    + "//" + currMap.substring(2);
            content.append(Utils.readContentsAsString(new File(currBlobFile)));
        }
        content.append("=======\n");
        if (givMap != null) {
            String givBlobFile = ".gitlet//objects//" + givMap.substring(0, 2)
                    + "//" + givMap.substring(2);
            content.append(Utils.readContentsAsString(new File(givBlobFile)));
        }
        content.append(">>>>>>>\n");
        Utils.writeContents(new File(fileName), content.toString());
    }

    /** Returns the split point between CURRENTCOMMIT and GIVENCOMMIT. */
    private static String getSplitPoint(String currentCommit,
                                        String givenCommit) {
        Commit currCommit = Commit.getCommit(currentCommit);
        Commit givCommit = Commit.getCommit(givenCommit);

        HashSet<String> currCommitAncestors = new HashSet<>();
        while (!currCommit.getParentID().equals("None")) {
            currCommitAncestors.add(currCommit.getID());
            currCommit = Commit.getCommit(currCommit.getParentID());
        }
        currCommitAncestors.add(currCommit.getID());
        while (!givCommit.getParentID().equals("None")) {
            if (currCommitAncestors.contains(givCommit.getID())) {
                return givCommit.getID();
            }
            givCommit = Commit.getCommit(givCommit.getParentID());
        }
        return givCommit.getID();
    }



    /** Adds remote with name NAME, and directory DIREC. */
    static void addRemote(String name, String direc) {
        String[] split = direc.split("/");
        String dir = split[0];
        for (int i = 1; i < split.length; i++) {
            dir = dir + File.separator + split[i];
        }
        RemoteStorer remoteStorer = getRemoteStorer();
        if (remoteStorer.contains(name)) {
            throw Utils.error("A remote with that name already exists.");
        }
        remoteStorer.add(name, dir);
        updateRemoteStorer(remoteStorer);
    }


    /** Removes remote with name NAME. */
    static void removeRemote(String name) {
        RemoteStorer remoteStorer = getRemoteStorer();
        if (!remoteStorer.contains(name)) {
            throw Utils.error("A remote with that name does not exist.");
        }
        remoteStorer.remove(name);
        updateRemoteStorer(remoteStorer);
    }

    /** Returns the remote storer. */
    private static RemoteStorer getRemoteStorer() {
        return Utils.readObject(new File(".gitlet//remotes"),
                RemoteStorer.class);
    }

    /** Updates the remote storer to REM. */
    private static void updateRemoteStorer(RemoteStorer rem) {
        Utils.writeObject(new File(".gitlet//remotes"), rem);
    }

    /** Push to BRANCHNAME on REMOTENAME. */
    static void push(String remoteName, String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        Remote remote = getRemoteStorer().getRemote(remoteName);
        String remDir = remote.getDirectory();
        File remFile = new File(remote.getDirectory());
        if (!remFile.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        if (!Utils.plainFilenamesIn(remote.getDirectory()
                + "//refs//heads").contains(branchName)) {
            addBranchToRemote(branchName, remDir);
            return;
        }


        File remBranchFile = getRemoteBranchFile(remote.getDirectory(),
                branchName);
        String remComID = Utils.readContentsAsString(remBranchFile);

        checkNeedPullBeforePush(remComID);

        Commit currCommit = Commit.getCommit(getCurrentCommitID());
        pushBlobs(currCommit, remDir);
        String comID = currCommit.getID();
        String remCurDir = remDir
                + "//objects//" + comID.substring(0, 2);

        writeCommit(remCurDir, comID, currCommit);

        Utils.writeContents(new File(remDir
                + "//refs//heads//" + branchName), comID);
    }

    /** Performs the pushing of the blobs in CURRCOMMIT
     *  to REMDIR. */
    private static void pushBlobs(Commit currCommit, String remDir) {
        for (String blobID: currCommit.getBlobs().values()) {
            String remBlobDirPath = remDir
                    + "//objects//" + blobID.substring(0, 2);
            String remBlobFilePath = remBlobDirPath
                    + "//" + blobID.substring(2);
            String localBlobFilePath = ".gitlet//objects//"
                    + blobID.substring(0, 2) + "//" + blobID.substring(2);
            File remBlobFile = new File(remBlobFilePath);
            new File(remBlobDirPath).mkdir();
            File localBlobFile = new File(localBlobFilePath);
            try {
                remBlobFile.createNewFile();
            } catch (IOException e) {
                throw Utils.error(e.getMessage());
            }
            Utils.writeContents(remBlobFile,
                    Utils.readContentsAsString(localBlobFile));
        }
    }

    /** Check if needs to pull before pushing.
     *  Check by see if REMCOMID is part of history
     *  of this branch. */
    private static void checkNeedPullBeforePush(String remComID) {
        HashSet<String> currCommitAncestors = new HashSet<>();
        Commit currCommit = Commit.getCommit(getCurrentCommitID());
        while (!currCommit.getParentID().equals("None")) {
            currCommitAncestors.add(currCommit.getID());
            currCommit = Commit.getCommit(currCommit.getParentID());
        }
        currCommitAncestors.add(currCommit.getID());
        if (!currCommitAncestors.contains(remComID)) {
            throw Utils.error(
                    "Please pull down remote changes before pushing.");
        }
    }

    /** Fetch from BRANCHNAME from REMOTENAME. */
    static void fetch(String remoteName, String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        Remote remote = getRemoteStorer().getRemote(remoteName);
        if (remote == null) {
            throw Utils.error("A remote with that name does not exist.");
        } else {
            File remFile = new File(remote.getDirectory());
            if (!remFile.exists()) {
                throw Utils.error("Remote directory not found.");
            }
        }
        String remDir = remote.getDirectory();
        File remBranchFile = getRemoteBranchFile(remDir,
                branchName);
        String localBranchName = remoteName + "_" + branchName;
        if (!getAllBranches().contains(localBranchName)) {
            branch(localBranchName);
        }
        String comID = Utils.readContentsAsString(remBranchFile);
        Commit remCommit = getRemoteCommit(comID,
                remDir);

        fetchBlobs(remCommit, remDir);

        String comDir = ".gitlet//objects//" + comID.substring(0, 2);
        writeCommit(comDir, comID, remCommit);
        setBranchHead(BRANCH_HEADS_DIR + "//"
                + localBranchName, comID);
    }


    /** Writes the commit COMMIT with id COMID, to directory DIR. */
    private static void writeCommit(String dir, String comID, Commit commit) {
        new File(dir).mkdir();
        File comFile = new File(dir + "//" + comID.substring(2));
        try {
            comFile.createNewFile();
        } catch (IOException e) {
            throw Utils.error(e.getMessage());
        }
        Utils.writeObject(comFile,
                commit);
    }

    /** Performs the fetching of the blobs in REMCOMMIT from
     *  REMDIR. */
    private static void fetchBlobs(Commit remCommit, String remDir) {

        for (String blobID : remCommit.getBlobs().values()) {
            String remBlobFilePath = remDir
                    + "//objects//" + blobID.substring(0, 2)
                    + "//" + blobID.substring(2);
            String localBlobDir = ".gitlet//objects//"
                    + blobID.substring(0, 2);
            String localBlobFilePath = localBlobDir + "//"
                    + blobID.substring(2);

            File remBlobFile = new File(remBlobFilePath);
            File localBlobDirFile = new File(localBlobDir);
            localBlobDirFile.mkdir();
            File localBlobFile = new File(localBlobFilePath);
            try {
                localBlobFile.createNewFile();
            } catch (IOException e) {
                throw Utils.error(e.getMessage());
            }
            Utils.writeContents(localBlobFile,
                    Utils.readContentsAsString(remBlobFile));
        }
    }


    /** Returns the commit of the remote with SHA1 ID,
     *  and remote directory DIR. */
    private static Commit getRemoteCommit(String sha1,
                                             String dir) {
        return Commit.getRemCommit(sha1, dir);
    }

    /** Returns file of remote branch with directory DIR,
     *  and branch name BRANCHNAME. */
    private static File getRemoteBranchFile(String dir,
                                            String branchName) {
        File branch = new File(dir + "//refs//heads//" + branchName);
        if (!branch.exists()) {
            throw Utils.error("That remote does not have that branch.");
        }
        return branch;
    }

    /** Add branch NAME to remote with directory DIR. */
    private static void addBranchToRemote(String name, String dir) {
        createFile(dir + "//refs//heads", name);
        setBranchHead(dir + "//refs//heads//" + name,
                getCurrentCommitID());
    }

    /** Pull from BRANCHNAME in REMOTENAME. */
    static void pull(String remoteName, String branchName) {
        if (branchName.contains("/")) {
            String[] split = branchName.split("/");
            branchName = split[0] + "_" + split[1];
        }
        fetch(remoteName, branchName);
        merge(remoteName + "_" + branchName);
    }

    /** Creates files inside of DIRNAME, with file name FILENAME.
     *  Assumes that the directory already exists.
     */
    private static void createFile(String dirName, String fileName) {
        try {
            assert (new File(dirName).exists()) : dirName
                    + "directory does not exist";
            File file = new File(dirName + "//" + fileName);
            if (!file.createNewFile()) {
                Utils.message("File already exists.");
            }
        } catch (IOException e) {
            Utils.message("File creation unsuccessful.");
            System.exit(1);
        }
    }

    /** Creates the director DIRNAME. */
    private static void createDir(String dirName) {
        try {
            File file = new File(dirName);
            if (!file.mkdir()) {
                throw new IOException();
            }
        } catch (IOException e) {
            Utils.message("Directory creation unsuccessful.");
            System.exit(1);
        }
    }

    /** Returns the current branch file path. */
    private static String getCurrentBranch() {
        return Utils.readContentsAsString(new File(".gitlet//HEAD"));
    }

    /** Returns the current branch name. */
    private static String getCurrentBranchName() {
        return getCurrentBranch().substring(
                BRANCH_HEADS_DIR.length() + 2);
    }

    /** Returns the current commit ID. */
    private static String getCurrentCommitID() {
        String branch = getCurrentBranch();
        return Utils.readContentsAsString(new File(branch));
    }

    /** Sets the COMMIT as head of BRANCH. */
    private static void setBranchHead(String branch, String commit) {
        Utils.writeContents(new File(branch), commit);
    }

    /** Sets BRANCH as the head. */
    private static void setHead(String branch) {
        Utils.writeContents(new File(".gitlet//HEAD"), branch);
    }

    /** Returns the stage. */
    private static Stage getStage() {
        return Utils.readObject(new File(STAGE_FILE), Stage.class);
    }

    /** Returns list of all branches. */
    private static List<String> getAllBranches() {
        return Utils.plainFilenamesIn(BRANCH_HEADS_DIR);
    }

    /** Returns the string from reading the file of BLOBID. */
    private static String readBlobFile(String blobID) {
        File file = new File(String.format(".gitlet//objects//%s//%s",
                blobID.substring(0, 2), blobID.substring(2)));
        return Utils.readContentsAsString(file);
    }

    /** Returns the deleted but not staged files of the repo
     *  by comparing STAGE with WORKDIRFILES. */
    private static Set<String> getDelNotStaged(Stage stage,
                                               List<String> workDirFiles) {
        ArrayList<String> result = new ArrayList<>();
        for (String trackedFile: stage.getTracked()) {
            if (!workDirFiles.contains(trackedFile)
                    && !stage.getRemoved().contains(trackedFile)) {
                result.add(trackedFile);
            }
        }
        return new TreeSet<>(result);
    }

    /** Returns the modified but not staqed files in
     *  STAGE by looking at WORKDIRFILES. */
    private static Set<String> getModNotStaged(Stage stage,
                                               List<String> workDirFiles) {
        HashMap<String, String> staged = stage.getStagedMap();
        HashMap<String, String> tracked = stage.getTrackedMap();
        ArrayList<String> result = new ArrayList<>();
        for (String fileName: workDirFiles) {
            if (staged.size() != 0 && staged.containsKey(fileName)) {
                if (!staged.get(fileName)
                        .equals(Blob.getSha1(new File(fileName)))) {
                    result.add(fileName);
                }
                continue;
            }
            if (tracked.size() != 0 && tracked.containsKey(fileName)) {
                if (!tracked.get(fileName)
                        .equals(Blob.getSha1(new File(fileName)))) {
                    result.add(fileName);
                }
            }
        }
        return new TreeSet<>(result);
    }

    /** Returns the untracked files in STAGE by looking at WORKDIRFILES. */
    private static Set<String> getUntrackedFiles(Stage stage,
                                                 List<String> workDirFiles) {
        ArrayList<String> result = new ArrayList<>();
        for (String fileName: workDirFiles) {
            if (stage.getStaged().contains(fileName)) {
                continue;
            }
            if (!stage.getTracked().contains(fileName)) {
                result.add(fileName);
            } else {
                if (stage.getRemoved().contains(fileName)) {
                    result.add(fileName);
                }
            }
        }
        return new TreeSet<>(result);
    }

    /** Returns full ID from the abbreviated commit ID ABBR. */
    private static String getFullID(String abbr) {
        File dir = new File(".gitlet//objects//" + abbr.substring(0, 2));
        if (!dir.exists()) {
            throw Utils.error("No commit with that id exists.");
        }
        List<String> commitFiles = Utils.plainFilenamesIn(".gitlet//objects//"
                + abbr.substring(0, 2));
        int match = 0;
        String id = "";
        for (String file : commitFiles) {
            if (file.startsWith(abbr.substring(2))) {
                match += 1;
                id = abbr.substring(0, 2) + file;
            }
        }
        if (match == 0) {
            throw Utils.error("No commit with that id exists.");
        } else if (match > 1) {
            throw Utils.error("Commit id not unique.");
        }
        return id;
    }

    /** Private class representing the commit log. */
    private static class CommitLog implements Serializable {

        /** Constructs an empty commit log. */
        CommitLog() {
            log = new HashSet<>();
        }

        /** Adds COMMITID to the commit log. */
        void add(String commitID) {
            log.add(commitID);
        }

        /** Returns the log. */
        HashSet<String> getLog() {
            return log;
        }

        /** The log storing the commits. */
        private HashSet<String> log;
    }

}

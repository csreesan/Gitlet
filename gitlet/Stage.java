package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;


/** Class representing the staging area.
 *  @author Chris Sreesangkom
 */
class Stage implements Serializable {

    /** Contructs the stage of the CURRENTCOMMIT. */
    Stage(Commit currentCommit) {
        commit = currentCommit;
        previous = currentCommit.getBlobs();
        added = new HashMap<>();
        modified = new HashMap<>();
        removed = new HashMap<>();
    }

    /** Method for adding the file with name
     *  FILENAME to the staging area. */
    void add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }
        String fileSha1 = Blob.getSha1(file);
        if (removed.containsKey(fileName)) {
            removed.remove(fileName);
        }
        if (previous.containsKey(fileName)) {
            if (previous.get(fileName).equals(fileSha1)) {
                if (modified.containsKey(fileName)) {
                    modified.remove(fileName);
                }
            } else {
                modified.put(fileName, fileSha1);
                Blob.createBlobObj(file);
            }
        } else {
            added.put(fileName, fileSha1);
            Blob.createBlobObj(file);
        }
    }

    /** Method for removing the file with name
     *  FILENAME from the staging area returning
     *  TRUE if there is a change and FALSE
     *  otherwise. */
    void remove(String fileName) {
        if (!previous.containsKey(fileName)
                && !added.containsKey(fileName)) {
            Utils.message("No reason to remove the file.");
        }
        if (previous.containsKey(fileName)) {
            if (!removed.containsKey(fileName)) {
                removed.put(fileName, previous.get(fileName));
                Utils.restrictedDelete(fileName);
            } else if (modified.containsKey(fileName)) {
                modified.remove(fileName);
            }
        }
        if (added.containsKey(fileName)) {
            added.remove(fileName);
        }
    }

    /** Method for commiting the stage for
     *  the commit CURRENTCOMMIT with commit
     *  message MESSAGE. Returns a string of
     *  commit. */
    String commitStage(String message) {
        long time = Instant.now().toEpochMilli();
        HashMap<String, String> commitBlobs = new HashMap<>();
        for (String file :previous.keySet()) {
            if (!removed.containsKey(file)) {
                commitBlobs.put(file, previous.get(file));
            }
        }
        commitBlobs.putAll(modified);
        commitBlobs.putAll(added);

        return Commit.createCommit(commitBlobs,
                commit.getID(), time , message);
    }

    /** Method for making a merge commit for
     *  the commit CURRENTCOMMIT with commit
     *  message MESSAGE and GIVENBRANCHID
     *  as the second parent. Returns a string of
     *  the commit. */
    String commitStage(String message,
                       String givenBranchID) {
        long time = Instant.now().toEpochMilli();
        HashMap<String, String> commitBlobs = new HashMap<>();
        for (String file :previous.keySet()) {
            if (!removed.containsKey(file)) {
                commitBlobs.put(file, previous.get(file));
            }
        }
        commitBlobs.putAll(modified);
        commitBlobs.putAll(added);

        return Commit.createCommit(commitBlobs,
                commit.getID(), time , message,
                givenBranchID);
    }

    /** Returns a boolean indicating whether
     *  there has been a change from the previous
     *  commit. */
    boolean getChanged() {
        return !((removed.size() + added.size()
                + modified.size()) == 0);
    }

    /** Returns the set of the file names
     *  of the staged files. */
    Set<String> getStaged() {
        TreeSet<String> staged = new TreeSet<>();
        staged.addAll(modified.keySet());
        staged.addAll(added.keySet());
        return staged;
    }

    /** Returns the hashmap of the file names
     *  and ID of the staged files. */
    HashMap<String, String> getStagedMap() {
        HashMap<String, String> result = new HashMap<>();
        result.putAll(modified);
        result.putAll(added);
        return result;
    }

    /** Returns the set of the file names
     *  of the removed files. */
    Set<String> getRemoved() {
        return new TreeSet<>(removed.keySet());
    }

    /** Returns the set of the file names
     *  of the tracked file. */
    Set<String> getTracked() {
        return new TreeSet<>(previous.keySet());
    }

    /** Returns the hashmap of the file names
     *  and ID of the tracked files. */
    HashMap<String, String> getTrackedMap() {
        return previous;
    }

    /** The current commit of the stage. */
    private Commit commit;

    /** Map paring of previous commit file names and its ID. */
    private HashMap<String, String> previous;

    /** Map paring of added file names and its ID. */
    private HashMap<String, String> added;

    /** Map paring of modified file names and its ID. */
    private HashMap<String, String> modified;

    /** Map paring of removed file names and its ID. */
    private HashMap<String, String> removed;
}

package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** Class representing a Commit.
 *  @author Chris Sreesangkom
 */
class Commit implements Serializable {

    /** Contstructor for a normal commit with
     *  staging area STAGED,
     *  parent ID PARID,
     *  timestampe TIME,
     *  commit message MESS. */
    private Commit(HashMap<String, String> staged,
                   String parID, long time, String mess) {
        blobs = staged;
        parentID = parID;
        secondParentID = null;
        timestamp = time;
        message = mess;
        sha1 = Utils.sha1(staged.toString(),
                parID, Long.toString(time), mess);
        String dirName = ".gitlet//objects//" + sha1.substring(0, 2);
        String fileName = ".gitlet//objects//"
                + sha1.substring(0, 2) + "//" + sha1.substring(2);
        try {
            new File(dirName).mkdir();
            Utils.writeObject(new File(fileName), this);
        } catch (IllegalArgumentException e) {
            throw Utils.error("Commit creation unsuccessful");
        }
    }

    /** Contstructor for a merged commit with
     *  staging area STAGED,
     *  parent ID PARID,
     *  timestampe TIME,
     *  commit message MESS
     *  second parent DI SECPARID. */
    private Commit(HashMap<String, String> staged,
                   String parID, long time, String mess,
                   String secParID) {
        blobs = staged;
        parentID = parID;
        secondParentID = secParID;
        timestamp = time;
        message = mess;
        sha1 = Utils.sha1(staged.toString(),
                parID, secParID, Long.toString(time), mess);
        String dirName = ".gitlet//objects//" + sha1.substring(0, 2);
        String fileName = ".gitlet//objects//"
                + sha1.substring(0, 2) + "//" + sha1.substring(2);
        try {
            new File(dirName).mkdir();
            Utils.writeObject(new File(fileName), this);
        } catch (IllegalArgumentException e) {
            throw Utils.error("Commit creation unsuccessful");
        }
    }

    /** Returns a commit with the SHA1 as ID. */
    static Commit getCommit(String sha1) {
        String file = ".gitlet//objects//" + sha1.substring(0, 2)
                + "//" + sha1.substring(2);
        try {
            return Utils.readObject(new File(file), Commit.class);
        } catch (IllegalArgumentException e) {
            throw Utils.error("No commit with that id exists.");
        }
    }

    /** Returns a commit with the SHA1 as REMOTEDIR. */
    static Commit getRemCommit(String sha1, String remoteDir) {
        String file = remoteDir + "//objects//"
                + sha1.substring(0, 2)  + "//" + sha1.substring(2);
        try {
            return Utils.readObject(new File(file), Commit.class);
        } catch (IllegalArgumentException e) {
            throw Utils.error("No commit with that id exists.");
        }
    }


    /** Creates the commit with
     *  staging area STAGED,
     *  parent ID PARENTID,
     *  timestampe TIME,
     *  commit message MESSAGE.
     *  Returns the ID of the commit created. */
    static String createCommit(HashMap<String, String> staged,
                               String parentID, long time,
                               String message) {
        return new Commit(staged, parentID, time, message).getID();
    }

    /** Creates the merged commit with
     *  staging area STAGED,
     *  parent ID PARENTID,
     *  timestampe TIME,
     *  commit message MESSAGE
     *  second parent DI SECONDPARENTID.
     *  Returns the ID of the commit created. */
    static String createCommit(HashMap<String, String> staged,
                               String parentID, long time,
                               String message, String secondParentID) {
        return new Commit(staged, parentID, time, message,
                secondParentID).getID();
    }

    /** Returns the commit message. */
    String getMessage() {
        return message;
    }

    /** Returns the commit time. */
    String getTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat(
                "E MMM d hh:mm:ss yyyy Z");
        return timeFormat.format(new Date(timestamp));
    }

    /** Returns the ID of the parent commit. */
    String getParentID() {
        return parentID;
    }

    /** Returns the ID of the second parent commit. */
    String getSecondParentID() {
        return secondParentID;
    }

    /** Returns boolean indicating whether it is
     *  a merge commit. */
    boolean isMergeCommit() {
        return secondParentID != null;
    }

    /** Returns the mapping of file names at blob ID. */
    HashMap<String, String> getBlobs() {
        return blobs;
    }

    /** Returns the commit ID. */
    String getID() {
        return sha1;
    }

    /** Overwrites parentID with ID. */
    void setParent(String id) {
        parentID = id;
    }

    /** Mapping of file name and blob ID. */
    private HashMap<String, String> blobs;
    /** The ID of the parent commit. */
    private String parentID;
    /** The ID ofthe second parent commit.
     *  Null if not a merged commit. */
    private String secondParentID;
    /** Timestamp of the commit. */
    private long timestamp;
    /** Commit message. */
    private String message;
    /** Commit ID. */
    private String sha1;
}

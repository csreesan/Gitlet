package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** Class the stores all the remotes in
 *  a set.
 *  @author Chris Sreesangkom */
class RemoteStorer implements Serializable {

    /** Constructs an empty remote set. */
    RemoteStorer() {
        remoteMap = new HashMap<>();
    }

    /** Add a remote to the repository. The remote will
     *  have the name NAME, and the directory of
     *  the repo DIR. */
    void add(String name, String dir) {
        remoteMap.put(name, new Remote(name, dir));
    }

    /** Removes the remote NAME from the repository. */
    void remove(String name) {
        remoteMap.remove(name);
    }

    /** Returns boolean if the remote with name NAME is
     *  in the storer. */
    boolean contains(String name) {
        return remoteMap.containsKey(name);
    }

    /** Returns the remote with name NAME. */
    Remote getRemote(String name) {
        return remoteMap.get(name);
    }


    /** A set which stores all the remotes for the
     *  repository. */
    private HashMap<String, Remote> remoteMap;

}

package gitlet;


import java.io.Serializable;

/** A class representing each remote.
 *  @author Chris Sreesangkom. */
class Remote implements Serializable {


    /** Constructs a remote with NME and DIR directory. */
    Remote(String nme, String dir) {
        directory = dir;
        name = nme;
    }

    /** Returns the name of the remote. */
    String getName() {
        return name;
    }

    /** Returns the directory of the remote. */
    String getDirectory() {
        return directory;
    }

    /** The name of the remote. */
    private String name;
    /** The directory of the remote. */
    private String directory;


}

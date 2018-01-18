package gitlet;


import java.io.File;
import java.io.IOException;

/** Class representing Blob storing
 *  file contents.
 *  @author Chris Sreesangkom
 */
class Blob {

    /** Returns the Sha-1 encoding of FILE. */
    static String getSha1(File file) {
        String fileContent = Utils.readContentsAsString(file);
        return Utils.sha1("blob", file.toPath().toString(), fileContent);
    }

    /** Creates the blob for FILE. */
    static void createBlobObj(File file) {
        String sha1 = getSha1(file);
        String fileName = ".gitlet//objects" + "//"
                + sha1.substring(0, 2) + "//" + sha1.substring(2);
        String dirName = ".gitlet//objects" + "//" + sha1.substring(0, 2);
        try {
            new File(dirName).mkdir();
            File blobFile = new File(fileName);
            if (!blobFile.createNewFile()) {
                return;
            }
            Utils.writeContents(blobFile, Utils.readContentsAsString(file));
        } catch (IOException e) {
            throw Utils.error("Blob creation unsuccessful.");
        }
    }

}

package gitlet;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.

 *  does at a high level.
 *
 *  @author Dat Nguyen
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date date;
    private String id;
    private HashMap<String, String> blobs;
    private Commit parentOne;
    private Commit parentTwo;

    public Commit(String message) {
        this.message = message;
        this.date = new Date();
        blobs = new HashMap<>();
        parentOne = null;
        parentTwo = null;
        this.id = Utils.sha1(message, date.toString());
    }
    public Commit(String message, Date date) {
        this.message = message;
        this.date = date;
        blobs = new HashMap<>();
        parentOne = null;
        parentTwo = null;
        this.id = Utils.sha1(message, this.date.toString());
    }
    public Commit(String message, HashMap<String, String> blobs, Commit parent) {
        this.message = message;
        this.date = new Date();
        this.blobs = new HashMap<>();
        Set<String> fileNames = blobs.keySet();
        for (String name : fileNames) {
            this.blobs.put(name, blobs.get(name));
        }
        this.parentOne = parent;
        parentTwo = null;

        this.id = Utils.sha1(message, date.toString());
    }

    public Commit getParent() {
        return parentOne;
    }
    public void setParent(Commit parent) {
        this.parentOne = parent;
    }
    public void setParentTwo(Commit parent) {
        this.parentTwo = parent;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public void removeBlob(String name) {
        blobs.remove(name);
    }

    public String id() {
        return id;
    }
    public void addBlob(File file) {
        String name = file.getName();
        String content = Utils.readContentsAsString(file);
        String code = Utils.sha1(content);
        blobs.put(name, code);  // add to blobs hashmap

        File f = Utils.join(Repository.GITLET_DIR, code);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            Utils.writeContents(f, content);
        }
    }

    public void addBlob(File file, String fileName) {
        blobs.put(fileName, file.getName());
    }

    public String getSHA1FileName(String fileName) {
        return blobs.get(fileName);
    }
}

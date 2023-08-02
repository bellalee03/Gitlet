package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

public class Commit implements Serializable {

    /** the time when the commit was made. */
    private String _time;

    /** message of the commit. */
    private String _message;

    /** parent commit's sha1. */
    private String _parentSha1;

    /** second parent's sha1. */
    private String _parentSha2;

    /** <fileName, Blob>. Stores blob objects. */
    private TreeMap<String, Blob> _blobObject;

    /** sha1 of the commit. */
    private String _comSha1;

    /** shows if the commit is a merge commit. */
    private boolean mergeCommit = false;

    public Commit(String message, String parentSha1,
                  TreeMap<String, Blob> blob) {
        if (parentSha1 == null) {
            _time = "Thu Jan 1 00:00:00 1970 -0800";
        } else {
            _parentSha1 = parentSha1;
            Date curr = new Date(0);
            String pattern = "EEE MMM dd HH:mm:ss yyyy Z";
            SimpleDateFormat result = new SimpleDateFormat(pattern);
            _time = result.format(curr);
        }
        _message = message;
        _comSha1 = Utils.sha1(Utils.serialize(this));
        _blobObject = blob;
    }

    public TreeMap<String, Blob> getTree() {
        return _blobObject;
    }

    public boolean blobExists(String fileName) {
        return _blobObject.containsKey(fileName);
    }

    public Blob getBlob(String fileName) {
        return _blobObject.get(fileName);
    }

    public String getSha1() {
        return _comSha1;
    }

    public String getTime() {
        return _time;
    }

    public String getMessage() {
        return _message;
    }

    public String getParentSha1() {
        return _parentSha1;
    }

    public String getParentSha2() {
        return _parentSha2;
    }

    public void addParent2(String parent) {
        mergeCommit = true;
        _parentSha2 = parent;
    }

}

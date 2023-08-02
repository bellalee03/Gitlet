package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    /** sha1 of the blob object. */
    private String _blobSha1;

    /** the content of the blob, stored as byte[]. */
    private byte[] _content;

    /** content of the blob, stored as String. */
    private String _contString;

    /** the file that blob is storing. */
    private File _file;

    public Blob(File file) {
        _file = file;
        _content = Utils.readContents(file);
        _contString = Utils.readContentsAsString(file);
        _blobSha1 = Utils.sha1(Utils.serialize(_content));
    }

    public String getBlobSha1() {
        if (this.equals(null)) {
            return null;
        } else {
            return _blobSha1;
        }
    }

    public String contString() {
        return _contString;
    }

    public boolean blobEquals(Blob blob1) {
        if (blob1 != null && this != null) {
            return blob1.getBlobSha1().equals(this.getBlobSha1());
        } else if (blob1.equals(null) && this.equals(null)) {
            return true;
        }
        return false;
    }

    public byte[] getContent() {
        return _content;
    }

}

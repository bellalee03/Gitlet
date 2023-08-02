package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class StagingArea implements Serializable {

    /** tracked file, key: name, value: sha1 .*/
    private HashMap<String, String> addingStage;

    /** untracked file, key: name, value: sha1. */
    private HashMap<String, String> removingStage;

    public StagingArea() {
        addingStage = new HashMap<String, String>();
        removingStage = new HashMap<String, String>();
    }

    public void addFileToStage(String tracking, String fileName, String sha1) {
        if (tracking == "add") {
            addingStage.put(fileName, sha1);
        } else if (tracking == "remove") {
            removingStage.put(fileName, sha1);
        } else {
            System.out.println("File must be added/removed.");
        }
    }

    public void reset() {
        addingStage = new HashMap<String, String>();
        removingStage = new HashMap<String, String>();
    }

    public String getSha1(String tracking, String fileName) {
        if (tracking == "add") {
            return addingStage.get(fileName);
        } else if (tracking == "remove") {
            return removingStage.get(fileName);
        } else {
            return "Stage should be adding/removing/modified stage.";
        }
    }

    public java.util.HashMap<String, String> getAddingStage() {
        return addingStage;
    }

    public HashMap<String, String> getRemovingStage() {
        return removingStage;
    }
}

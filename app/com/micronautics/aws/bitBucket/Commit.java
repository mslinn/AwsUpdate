package com.micronautics.aws.bitBucket;

import java.util.TreeMap;

public class Commit {
    /** Sorted map of file path to action, where action is one of: "added", "deleted".
     * map is sorted by key (file path) */
    public TreeMap<String, String> filesToActions = new TreeMap<String, String>();

    /** Name of repository, must be same as repoName of AWS S3 bucket */
    public String repoName = "";

    /** Name of repo owner */
    public String ownerName = "";
}

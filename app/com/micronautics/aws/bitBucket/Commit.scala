package com.micronautics.aws.bitBucket;

import java.util.TreeMap;

// TODO this raw port from Java could be made more functional

case class Commit(
    /** Name of repository, must be same as repoName of AWS S3 bucket */
    repoName: String = "",

    /** Name of repository owner */
    ownerName: String = "",
    
    /** Sorted map of file path to action, where action is one of: "added", "deleted".
     * map is sorted by key (file path) */
    filesToActions: TreeMap[String, String] = new TreeMap[String, String]())

package com.micronautics.aws.bitBucket;

import com.micronautics.aws.S3;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class BBCopy implements Callable<File> {
    File tmpDir;
    Commit commit;
    String fileName;
    BitBucketOAuth bitBucket;
    static S3 s3 = new S3();
    BitBucketBasicAuth bitBucketBasicAuth = new BitBucketBasicAuth(s3);

    public BBCopy(File tmpDir, Commit commit, String fileName) throws IOException {
        this.tmpDir = tmpDir;
        this.commit = commit;
        this.fileName = fileName;
    }

    public File call() {
        try {
            //String urlStr = "https://bitbucket.org/" + commit.ownerName + "/" + commit.repoName + "/raw/master/" + fileName;
            String fileMetaJson = bitBucketBasicAuth.urlStrSrc(commit.ownerName, commit.repoName, fileName);
            int fileSize = JSON.parseFileSize(fileMetaJson, fileName);
            String rawFileUrl = bitBucketBasicAuth.urlStrRaw(commit.ownerName, commit.repoName, fileName);
            String action = commit.filesToActions.get(fileName); // todo figure out how to handle
            //result += "  " + fileName + ": " + commit.filesToActions.get(fileName) + " " + fileSize + "bytes from " + rawFileUrl + "\n";
            System.out.println("Copying '" + fileName + "' (" + fileSize + " bytes)");
            s3.uploadStream(commit.repoName, fileName, bitBucketBasicAuth.getInputStream(rawFileUrl), fileSize);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

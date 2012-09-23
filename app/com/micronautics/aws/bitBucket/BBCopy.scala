package com.micronautics.aws.bitBucket

import java.io.File
import java.util.concurrent.Callable
import BBCopy._
import model.Model._


// TODO this raw port from Java could be made more functional

/** Only supports one BitBucket account per application */
object BBCopy {
    val bitBucketBasicAuth = new BitBucketBasicAuth(s3)

  def apply(tmpDir: File, commit: Commit, fileName: String): BBCopy = {
    if (bitBucketBasicAuth.exception!=null)
      throw bitBucketBasicAuth.exception
    new BBCopy(tmpDir, commit, fileName)
  }
}

class BBCopy(val tmpDir: File, val commit: Commit, val fileName: String) extends Callable[Unit] {

    /** Read file corresponding to fileName from repository into a temporary file, stream to AWS S3, then delete temporary file */
    // TODO use stream to stream copy instead
    def call(): Unit = {
        try {
            val fileMetaJson: String = bitBucketBasicAuth.dirMetadata(commit.ownerName, commit.repoName, fileName)
            val fileSize: Int = JSON.parseFileSize(fileMetaJson, fileName)
            val rawFileUrl: String = bitBucketBasicAuth.urlStrRaw(commit.ownerName, commit.repoName, fileName)
            val action: String = commit.filesToActions.get(fileName); // TODO figure out how to handle various actions
            System.out.println("Copying '" + fileName + "' (" + fileSize + " bytes) to bucket '" + commit.repoName + "', owned by '" + commit.ownerName + "'")
            // TODO ensure bucket exists
            s3.uploadStream(commit.repoName.toLowerCase, fileName, bitBucketBasicAuth.getInputStream(rawFileUrl), fileSize)
        } catch {
          case ex: Exception =>
            println("BBCopy.call() caught an exception")
            Console.err.println("BBCopy.call() " + (if (ex.getMessage.length==0) ex.toString else ex.getMessage))
        } finally {
          val file = new File(fileName)
          if (file.exists)
            file.delete
        }
    }
}

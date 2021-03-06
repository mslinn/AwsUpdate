package com.micronautics.aws.bitBucket

import BitBucketBasicAuth._
import java.io.File
import java.util.concurrent.Callable
import model.Model._


// TODO this raw port from Java could be made more functional

/** Only supports one BitBucket account per application */
object BBCommitHandler {
  def apply(commit: Commit, fileName: String): BBCommitHandler = {
    if (bitBucketBasicAuth.exception!=null)
      throw bitBucketBasicAuth.exception
    new BBCommitHandler(commit, fileName)
  }
}

/** @param fileName is fully qualified without a leading slash */
class BBCommitHandler(val commit: Commit, val fileName: String) extends Callable[Unit] {
  /** <pre>{
      "node": "ee510da4ba9e",
      "path": "/",
      "directories": [
          "Scripts",
          "_notes",
          "expert",
          "images",
          "sites",
          "testimonials"
      ],
      "files": [
          {
              "size": 51,
              "path": ".gitignore",
              "timestamp": "2012-09-23 19:57:07",
              "utctimestamp": "2012-09-23 19:57:07+00:00",
              "revision": "ee510da4ba9e"
          }, ...
      ]
  }</pre> */
  val fileMetaJson: String = bitBucketBasicAuth.dirMetadata(commit.ownerName, commit.repoName, fileName)
  if (fileMetaJson=="Not Found")
    println("No files have been checked into the repository.")

    /** Read file corresponding to fileName from repository into a temporary file, stream to AWS S3, then delete temporary file */
    def call(): Unit = {
        try {
            val fileSize: Int = JSON.parseFileSize(fileMetaJson, fileName)
            val rawFileUrl: String = urlStrRaw(commit.ownerName, commit.repoName, fileName)
            val action: String = commit.filesToActions.get(fileName)
            // TODO ensure bucket exists
            action match {
              case "added" =>
                println("Copying new file '" + fileName + "' (" + fileSize + " bytes) at " + rawFileUrl + " to bucket '" + commit.repoName + "', owned by '" + commit.ownerName + "'")
                s3.uploadStream(commit.repoName.toLowerCase, fileName, bitBucketBasicAuth.getInputStream(rawFileUrl), fileSize)

              case "modified" =>
                println("Copying modified file '" + fileName + "' (" + fileSize + " bytes) at " + rawFileUrl + " to bucket '" + commit.repoName + "', owned by '" + commit.ownerName + "'")
                s3.uploadStream(commit.repoName.toLowerCase, fileName, bitBucketBasicAuth.getInputStream(rawFileUrl), fileSize)

              case "removed" =>
                println("Deleting '" + fileName + "' from bucket '" + commit.repoName + "', owned by '" + commit.ownerName + "'")
                s3.deleteObject(commit.repoName.toLowerCase, fileName)

              case _ =>
                println("BBCommitHandler got an unexpected action '" + action + "'")
            }
        } catch {
          case ex: Exception =>
            println("BBCommitHandler.call() caught an exception")
            Console.err.println("BBCommitHandler.call() " + (if (ex.getMessage.length==0) ex.toString else ex.getMessage))
        } finally {
          val file = new File(fileName)
          if (file.exists)
            file.delete
        }
    }
}

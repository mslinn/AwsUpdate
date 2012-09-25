package com.micronautics.aws.bitBucket

import annotation.tailrec
import BitBucketBasicAuth._
import com.micronautics.aws.bitBucket.BitBucketBasicAuth._
import java.io.{File}
import model.Model._
import org.codehaus.jackson.{JsonNode, JsonParser}
import org.codehaus.jackson.map.ObjectMapper
import collection.JavaConversions._

/** Only supports one BitBucket account per application */
object BBCopy {
  def apply(repoName: String) {
    if (bitBucketBasicAuth.exception!=null)
      throw bitBucketBasicAuth.exception

    val ownerName = bitBucketBasicAuth.userid
    val fileMetaJson: String = bitBucketBasicAuth.dirMetadata(ownerName, repoName, "")
    if (fileMetaJson=="Not Found") {
      println("No files have been checked into the repository.")
    } else {
      try {
        copyDir("") // TODO use future
      } catch {
        case ex =>
          Console.err.println(ex.getMessage)
          //PreconditionFailed(ex.getMessage)
      }
    }

    /** Copies all files from BB repo to corresponding AWS S3 bucket; does not delete files from S3.
     * @param fileName is fully qualified without a leading slash */
    def copyOne(fileName: String, fileSize: Int): Unit = {
        try {
          val rawFileUrl: String = urlStrRaw(ownerName, repoName, fileName)
          println("  Copying file '" + fileName + "' (" + fileSize + " bytes) to bucket '" + repoName + "', owned by '" + ownerName + "'")
          s3.uploadStream(repoName.toLowerCase, fileName, bitBucketBasicAuth.getInputStream(rawFileUrl), fileSize)
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

    def copyDir(path: String): Unit = {
      val url: String = urlStrRaw(ownerName, repoName, path)
      val jsonDir: String = bitBucketBasicAuth.getUrlAsString(url)
      val mapper = new ObjectMapper
      var rootNode: JsonNode = mapper.readValue(jsonDir, classOf[JsonNode])

      val dirs = rootNode.path("directories")
      val files = rootNode.findValues("files")
      if (files.length>0) files(0).foreach { fileJson =>
        val fileName = fileJson.get("path").getTextValue
        val fileSize = fileJson.get("size").getIntValue
        copyOne(fileName, fileSize)
      }
      dirs.foreach { dirName =>
        println("Copying " + dirName.getTextValue)
        copyDir(dirName.getTextValue) }
    }
  }
}

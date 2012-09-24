package model

import com.amazonaws.services.s3.model.{ListBucketsRequest, Bucket}
import com.amazonaws.services.s3.AmazonS3Client
import com.micronautics.aws.S3
import scala.collection.JavaConversions._
import com.micronautics.aws.bitBucket.BitBucketBasicAuth

object Model {
  val s3 = new S3()
  val amazonS3Client = new AmazonS3Client(s3.awsCredentials)
  val bitBucketBasicAuth = new BitBucketBasicAuth(s3)
  val listBucketsRequest = new ListBucketsRequest

  /** Endpoint was not returned in BucketWebsiteConfiguration by AmazoneS3Client v3.14 - has this changed? */
  // TODO compute this value for each bucket
  def endpoint(bucketName: String): String = "s3-website-us-east-1.amazonaws.com"

  // TODO don't return a clickable link if the bucket is not configured as a web site
  def bucketUrl(bucketName: String): String =
    "http://" + bucketName + "." + endpoint(bucketName)

  /** Buckets can be created any time */
  def buckets = amazonS3Client.listBuckets(listBucketsRequest).toArray.toList.asInstanceOf[List[Bucket]]
}

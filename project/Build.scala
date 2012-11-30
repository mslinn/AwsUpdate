import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {
    val appName    = "AwsUpdate"
    val appVersion = "0.1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.micronautics" % "awss3"             % "0.1.0-SNAPSHOT" withSources(),
      "com.codahale"     % "jerkson_2.9.1"     % "0.5.0",
      "org.scalaj"       % "scalaj-time_2.9.1" % "0.6" withSources()
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += Resolver.url("ScalaSBT snapshots",
        url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    )
}

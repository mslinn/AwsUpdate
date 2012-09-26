import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {
    val appName    = "AwsUpdate"
    val appVersion = "0.1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.micronautics" %  "awss3"       % "0.1.0-SNAPSHOT" withSources(),
      "com.codahale"     %% "jerkson"     % "0.5.0",
      "org.scalaj"       %% "scalaj-time" % "0.6" withSources()
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += Resolver.url("sbt-plugin-snapshots",
        new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    )
}

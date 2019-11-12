import sbt._
import sbt.complete.Parser._
import sbt.complete._

name := """sos"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"


val webJars = Seq(
  "org.webjars.bower" % "bootstrap" % "4.1.0",
  "org.webjars" % "jquery" % "3.3.1"
)

val silhouetteVersion = "5.0.3"
val silhouetteDeps = Seq(
  "com.mohiva" %% "play-silhouette" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion
)

val scalaOAuthVersion = "1.3.0"
val scalaOAuthDeps = Seq(
  "com.nulab-inc" %% "scala-oauth2-core" % scalaOAuthVersion,
  "com.nulab-inc" %% "play2-oauth2-provider" % scalaOAuthVersion
)

val slickVersion = "3.0.1"
val slickDeps = Seq(
  "com.typesafe.play" %% "play-slick" % slickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % slickVersion
)

val mailerVersion = "6.0.1"
val mailerDeps = Seq(
  "com.typesafe.play" %% "play-mailer" % mailerVersion,
  "com.typesafe.play" %% "play-mailer-guice" % mailerVersion
)

libraryDependencies ++= webJars ++ silhouetteDeps ++ scalaOAuthDeps ++ slickDeps ++ mailerDeps ++
  Seq(
    ehcache, ws, filters, guice,
    "com.typesafe.play" %% "play-json" % "2.6.9",
    "net.codingwell" %% "scala-guice" % "4.1.0",
    "com.iheart" %% "ficus" % "1.2.0",
    "org.webjars" %% "webjars-play" % "2.6.3",
    "org.postgresql" % "postgresql" % "9.4.1208",
    "net.kaliber" %% "play-s3" % "9.0.0",
    "net.coobird" % "thumbnailator" % "0.4.8",
    "com.braintreepayments.gateway" % "braintree-java" % "2.73.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
  )

libraryDependencies += scalaVersion("org.scala-lang" % "scala-compiler" % _).value


resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += ("Atlassian Releases" at "https://maven.atlassian.com/public/")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.jcenterRepo



lazy val seedDataParser: Parser[String] = literal("Dev") | literal("Prod")

lazy val seedData = inputKey[Unit]("Seed default data (use as 'seedDataDev' and 'seedDataProd')")

seedData := Def.inputTaskDyn {
  val env = seedDataParser.parsed
  runTask(Compile, "tasks.SeedData", env.toLowerCase)
}.evaluated


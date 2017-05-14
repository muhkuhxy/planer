name := "planer"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

import com.typesafe.sbt.traceur.Import.TraceurKeys._

lazy val root = (project in file(".")).
   enablePlugins(PlayScala, SbtWeb).
   settings(
      sourceFileNames in traceur in Assets := Seq(
        "javascripts/assignees.js",
        "javascripts/planer.js",
        "javascripts/overview.js",
        "javascripts/territory.js"
      )
   )

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
   jdbc,
   "com.typesafe.play" %% "anorm" % "2.4.0",
   evolutions,
   "org.webjars.bower" % "bootstrap-sass" % "3.3.6",
   "org.webjars.npm" % "moment" % "2.11.1",
   "org.mindrot" % "jbcrypt" % "0.3m",
   "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
   "org.webjars.npm" % "rx" % "4.1.0"
)



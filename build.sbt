name := "planer"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val root = (project in file(".")).
  enablePlugins(PlayScala, SbtWeb)

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

scalacOptions ++= Seq(
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  "com.typesafe.play" %% "anorm" % "2.5.3",
  evolutions,
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.typelevel" %% "cats-core" % "1.0.0-RC1"
)


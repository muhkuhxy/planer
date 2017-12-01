name := "planer"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).
   enablePlugins(PlayScala, SbtWeb)

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
   jdbc,
   "com.typesafe.play" %% "anorm" % "2.4.0",
   evolutions,
   "org.mindrot" % "jbcrypt" % "0.3m",
   "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
)


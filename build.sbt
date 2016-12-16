name := "docless"

version := "1.0"

val circeVersion = "0.6.1"
val enumeratumVersion = "1.5.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test"
)

//val paradise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full


organization := "com.timeout"

name := "docless"

version := "1.0_SNAPSHOT"

val circeVersion = "0.6.1"
val enumeratumVersion = "1.5.1"
val catsVersion = "0.8.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
  "org.typelevel" %% "cats" % catsVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

//val paradise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full

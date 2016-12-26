organization := "com.timeout"

name := "docless"

version := "1.0_SNAPSHOT"

val circeVersion      = "0.6.1"
val enumeratumVersion = "1.5.1"
val catsVersion       = "0.8.1"

val readme = "README.md"

scalaVersion := "2.11.8"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect"         % scalaVersion.value,
  "com.chuusai"    %% "shapeless"            % "2.3.2",
  "com.beachape"   %% "enumeratum"           % enumeratumVersion,
  "com.beachape"   %% "enumeratum-circe"     % enumeratumVersion,
  "org.typelevel"  %% "cats"                 % catsVersion,
  "io.circe"       %% "circe-core"           % circeVersion,
  "io.circe"       %% "circe-parser"         % circeVersion,
  "io.circe"       %% "circe-generic"        % circeVersion,
  "org.scalatest"  %% "scalatest"            % "3.0.0" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test"
)

val genReadme =
  taskKey[Unit](s"Copy readme file to project root")

genReadme := {
  val _ = (tut in Compile).value
  val tutDir = tutTargetDirectory.value
  val log = streams.value.log

  log.info(s"Copying ${tutDir / readme} to ${file(".") / readme }")

  IO.copyFile(
    tutDir / readme,
    file(".") / readme
  )
}

tutSettings

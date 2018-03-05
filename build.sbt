
organization := "com.timeout"

name := "docless"

version := "0.6.0-SNAPSHOT"

val circeVersion      = "0.9.1"
val enumeratumVersion = "1.5.12"
val catsVersion       = "1.0.1"
val shapelessVersion  = "2.3.3"
val ammoniteVersion   = "1.0.5"

val readme     = "README.md"
val readmePath = file(".") / readme

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.9", "2.12.4")

useGpg := true
useGpgAgent := true

enablePlugins(TutPlugin)

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect"         % scalaVersion.value,
  "com.chuusai"    %% "shapeless"            % shapelessVersion,
  "com.beachape"   %% "enumeratum"           % enumeratumVersion,
  "com.beachape"   %% "enumeratum-circe"     % enumeratumVersion,
  "org.typelevel"  %% "cats-core"            % catsVersion,
  "org.typelevel"  %% "cats-kernel"          % catsVersion,
  "org.typelevel"  %% "cats-macros"          % catsVersion,
  "io.circe"       %% "circe-core"           % circeVersion,
  "io.circe"       %% "circe-parser"         % circeVersion,
  "io.circe"       %% "circe-generic"        % circeVersion,
  "org.scalatest"  %% "scalatest"            % "3.0.5" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test",
  "com.lihaoyi"    % "ammonite"              % ammoniteVersion % "test" cross CrossVersion.full
)

val predef = Seq(
  "import com.timeout.docless.schema._",
  "import com.timeout.docless.swagger._",
  "import cats._",
  "import cats.syntax.all._",
  "import cats.instances.all._"
)

initialCommands in (Test, console) +=
  s"""
    |ammonite.Main(predef="${predef.mkString(";")}").run()
  """.stripMargin

val copyReadme =
  taskKey[File](s"Copy readme file to project root")

copyReadme := {
  val _      = (tut in Compile).value
  val tutDir = tutTargetDirectory.value
  val log    = streams.value.log

  log.info(s"Copying ${tutDir / readme} to ${file(".") / readme}")

  IO.copyFile(
    tutDir / readme,
    readmePath
  )
  readmePath
}

val pandocReadme =
  taskKey[Unit](s"Add a table of content to the README using pandoc")

 pandocReadme := {
   val readme = copyReadme.value
   val log    = streams.value.log
   val cmd =
     s"pandoc -B doc/header.md -f markdown_github --toc -s -S $readme -o $readme"
   log.info(s"Running pandoc: $cmd}")
   try { new Fork(cmd, None) } catch {
     case e: java.io.IOException =>
       log.error(
         "You might need to install the pandoc executable! Please follow instructions here: http://pandoc.org/installing.html"
       )
       throw e
   }
 
 }

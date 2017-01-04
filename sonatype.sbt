sonatypeProfileName := "com.timeout"

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url>http://github.com/timeoutdigital/docless</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/timeoutdigital/docless</connection>
    <developerConnection>scm:git:git@github.com:timeoutdigital/docless.git</developerConnection>
    <url>http://github.com/timeoutdigital/docless</url>
  </scm>
  <developers>
    <developer>
      <id>afiore</id>
      <name>Andrea Fiore</name>
      <url>https://github.com/afiore</url>
    </developer>
  </developers>
}

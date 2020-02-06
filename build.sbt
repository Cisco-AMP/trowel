name := "trowel"
organization := "com.cisco.amp.trowel"

val buildNumber = if (sys.env.contains("BUILD_NUMBER")) sys.env("BUILD_NUMBER") else "0-SNAPSHOT"
version := s"""0.1.$buildNumber"""

scalaVersion := "2.13.1"
val akkaVersion = "2.6.0"

libraryDependencies ++= Seq(
  "com.github.pureconfig"   %% "pureconfig"           % "0.12.1",
  "com.github.scopt"        %% "scopt"                % "3.7.1",
  "com.typesafe.akka"       %% "akka-http"            % "10.1.10",
  "com.typesafe.akka"       %% "akka-http-spray-json" % "10.1.10",
  "com.typesafe.akka"       %% "akka-http-testkit"    % "10.1.10"   % "test",
  "com.typesafe.akka"       %% "akka-slf4j"           % akkaVersion,
  "ch.qos.logback"          % "logback-classic"       % "1.2.3",      //TODO check version
  "com.lightbend.akka"      %% "akka-stream-alpakka-amqp"   % "1.1.2",
  "com.typesafe.akka"       %% "akka-stream"          % akkaVersion,
  "com.typesafe.akka"       %% "akka-stream-testkit"  % akkaVersion % "test",

  "org.scalatest"           %% "scalatest"            % "3.0.8"     % Test,
  "org.mockito"             %% "mockito-scala"        % "1.7.1"     % Test
)

// set the main class for 'run/assembly'
mainClass in (Compile, run) := Some("com.cisco.amp.trowel.Main")
mainClass in assembly := Some("com.cisco.amp.trowel.Main")

// items after here can be factored out
//
import sbt.{Credentials, Resolver}

assemblyJarName       in assembly := s"${name.value}_${scalaVersion.value}-${version.value}.jar"
assemblyOption        in assembly := (assemblyOption in assembly).value.copy(includeScala = true, includeDependency = true)
// skip tests for assembly task
test in assembly := {}

addArtifact(artifact in(Compile, assembly), assembly)

val tryPublish = taskKey[Unit]("Publish an artifact, but do not fail when the version exists")

tryPublish := {
  publish.result.value match {
    case Inc(inc: Incomplete) =>
      inc.directCause match {
        case Some(throwable) if throwable.getMessage == "destination file exists and overwrite == false" =>
          println(s"Ignoring failure: ${throwable.getMessage}" )
        case Some(_) =>
          throw inc.directCause.get
        case None => Unit
      }
    case Value(_) => Unit
  }
}

resolvers ++= Seq(
  Resolver.mavenLocal,
  "Nexus Snapshots" at s"https://${repoHost}/repository/maven-snapshots",
  "Nexus Release"   at s"https://${repoHost}/repository/maven-releases"
)

credentials ++= credentialsFromEnv() ++ credentialsFromSbt() ++ credentialsFromMaven()

val repoRealm = "Sonatype Nexus Repository Manager"
val repoHost  = "nexus.engine.sourcefire.com"

// disable publishing the main jar produced by `package`, the docs, and the sources
publishArtifact in(Compile, packageBin) := false
publishArtifact in(Compile, packageDoc) := false
publishArtifact in(Compile, packageSrc) := false

publishTo := {
  if (isSnapshot.value) Some("snapshots" at s"https://${repoHost}/repository/maven-snapshots")
  else                  Some("releases"  at s"https://${repoHost}/repository/maven-releases" )
}

def credentialsFromEnv(): Seq[Credentials] = {
  if (sys.env.contains("REPO_USERNAME"))
    Seq(
      Credentials(
        repoRealm,
        repoHost,
        sys.env.getOrElse("REPO_USERNAME", ""),
        sys.env.getOrElse("REPO_PASSWORD", "")
      )
    )
  else
    Seq()
}

def credentialsFromSbt(): Seq[Credentials] = {
  val file = Path.userHome / ".sbt" / ".credentials"
  if (file.canRead) Seq(Credentials(file)) else Seq()
}

def credentialsFromMaven(): Seq[Credentials] = {
  val file = Path.userHome / ".m2" / "settings.xml"
  if (file.canRead) {
    val credentials_id = sys.env.getOrElse("MAVEN_REPO_ID", "nexus")
    for {
      s <- xml.XML.loadFile(file) \ "servers" \ "server"
      id = (s \ "id").text
      if id == credentials_id
      username = (s \ "username").text
      password = (s \ "password").text
    } yield Credentials(repoRealm, repoHost, username, password)
  } else
    Seq()
}

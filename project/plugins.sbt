resolvers += Resolver.url("bintray-sbt-plugins", url("https://dl.bintray.com/eed3si9n/sbt-plugins/"))(Resolver.ivyStylePatterns)
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"          % "0.14.10")
addSbtPlugin("com.github.sbt"   % "sbt-jacoco"            % "3.1.0" )

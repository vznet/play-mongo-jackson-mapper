resolvers ++= Seq(
    DefaultMavenRepository,
    Resolver.url("Play", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Typesafe Other Repository" at "http://repo.typesafe.com/typesafe/repo/",
    "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
)

addSbtPlugin("play" % "sbt-plugin" % "2.0")

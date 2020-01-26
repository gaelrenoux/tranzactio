logLevel := Level.Warn

// resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
// addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.7")

/* Quality control */
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

/* Debugging utilities */
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

/* Fast development */
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

/* Assembly */
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

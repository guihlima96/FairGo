allprojects {
    repositories { google(); mavenCentral() }
}
val buildRoot = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(buildRoot)
subprojects {
    layout.buildDirectory.value(buildRoot.dir(name))
    evaluationDependsOn(":app")
}
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

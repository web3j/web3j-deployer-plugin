@file:JvmName("DeployPlugin")

package org.web3j.deploy

import java.net.URL
import java.net.URLClassLoader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention

class DeployPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val urls: MutableList<URL> = mutableListOf()
        // Inject the deployer library into the project
        project.pluginManager.apply(JavaPlugin::class.java)
        project.dependencies.add("implementation", "org.web3j:web3j-deployer:"+ "4.8.8")


        // Adding runtime classpath of project
        project.afterEvaluate {
            val plugin = it.convention.getPlugin(JavaPluginConvention::class.java)
            plugin.sourceSets.forEach { sourceSet ->
                sourceSet.runtimeClasspath.forEach { path ->
                    urls.add(path.toURI().toURL())
                }
            }
        }

        project.tasks.create("deploy") { task ->
            val profileName = project.properties.getOrDefault("profile", null)?.toString()
            val packageName = project.properties.getOrDefault("package", null)?.toString()

            if (profileName == null || packageName == null) {
                println("Missing profile and/or package name parameters")
                return@create
            }

            task.doLast {
                val loader = URLClassLoader(urls.toTypedArray())

                val deployToolsClass = loader.loadClass(DeployTools::class.java.name)
                val deployToolsConstructor = deployToolsClass.getDeclaredConstructor()
                val deployTools = deployToolsConstructor.newInstance()
                val deploy = deployToolsClass.getDeclaredMethod(
                    "deploy",
                    String::class.java,
                    String::class.java
                )

                deploy.invoke(deployTools, profileName, packageName)
            }

            task.group = "web3j"
            task.dependsOn("build")
        }
    }
}
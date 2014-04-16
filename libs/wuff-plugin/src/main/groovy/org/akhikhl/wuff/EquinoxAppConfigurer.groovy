/*
 * wuff
 *
 * Copyright 2014  Andrey Hihlovskiy.
 *
 * See the file "LICENSE" for copying and usage permission.
 */
package org.akhikhl.wuff

import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

/**
 *
 * @author akhikhl
 */
class EquinoxAppConfigurer extends OsgiBundleConfigurer {

  EquinoxAppConfigurer(Project project) {
    super(project)
  }

  @Override
  protected void configure() {
    super.configure()
    // these tasks need to be configured early (not in configureTasks),
    // so that netbeans recognizes them and uses them.
    project.task 'run', type: JavaExec
    project.task 'debug', type: JavaExec
  }

  @Override
  protected void configureProducts() {

    project.products.beforeProductGeneration.each { obj ->
      if(obj instanceof Closure)
        obj()
    }

    project.products.productList.each { product ->
      def productConfigurer = new EquinoxProductConfigurer(project, getProductConfigPrefix(), product)
      productConfigurer.configure()
    } // each product
  }

  private void configureRunTasks() {

    String runDir = "${project.buildDir}/run"
    String runConfigDir = "${runDir}/configuration"
    File runConfigFile = new File("$runConfigDir/config.ini")
    String runPluginsDir = "$runDir/plugins"

    project.task('prepareRunConfig') {
      group = 'wuff'
      description = 'prepares run configuration in buildDir/run'
      dependsOn project.tasks.jar
      dependsOn project.tasks.wrapLibs
      inputs.files { project.configurations.runtime.files }
      outputs.files runConfigFile
      doLast {
        // need to delete config-subdirs, otherwise osgi uses cached bundles,
        // not the bundles updated by prepareRunConfig task
        new File(runConfigDir).with {
          if(it.exists())
            it.eachDir { f -> f.deleteDir() }
        }

        // key is plugin name, value is complete launch entry for configuration
        def bundleLaunchList = [:]

        def addBundle = { File file ->
          String pluginName = PluginUtils.getPluginName(file.name)
          if(bundleLaunchList.containsKey(pluginName))
            return
          String launchOption = ''
          if(pluginName == 'org.eclipse.equinox.ds' || pluginName == 'org.eclipse.equinox.common')
            launchOption = '@2:start'
          else if(pluginName == 'org.eclipse.core.runtime' || pluginName == 'jersey-core')
            launchOption = '@start'
          if(pluginName != PluginUtils.osgiFrameworkPluginName && !pluginName.startsWith(PluginUtils.equinoxLauncherPluginName))
            bundleLaunchList[pluginName] = "reference\\:file\\:${file.absolutePath}${launchOption}"
        }

        addBundle project.tasks.jar.archivePath

        File wrappedLibsDir = PluginUtils.getWrappedLibsDir(project)
        if(wrappedLibsDir.exists())
          wrappedLibsDir.eachFileMatch(~/.*\.jar/) { addBundle it }

        project.configurations.runtime.each {
          if(ManifestUtils.isBundle(project, it))
            addBundle it
        }

        if(project.run.language) {
          project.configurations.findAll({ it.name.endsWith("${PlatformConfig.current_os}_${PlatformConfig.current_arch}_${project.run.language}") }).each { config ->
            config.files.each { file ->
              def m = file.name =~ /([\da-zA-Z_.-]+?)/ + "\\.nl_${project.run.language}" + /-((\d+\.)+[\da-zA-Z_.-]*)/
              if(m) {
                String pluginName = m[0][1]
                if(project.configurations.runtime.files.find { PluginUtils.getPluginName(it.name) == pluginName })
                  addBundle file
              }
            }
          }
        }

        bundleLaunchList = bundleLaunchList.sort()

        runConfigFile.parentFile.mkdirs()
        runConfigFile.withPrintWriter { PrintWriter configWriter ->
          String eclipseApplicationId = PluginUtils.getEclipseApplicationId(project)
          if(eclipseApplicationId)
            configWriter.println "eclipse.application=$eclipseApplicationId"
          String eclipseProductId = PluginUtils.getEclipseProductId(project)
          if(eclipseProductId)
            configWriter.println "eclipse.product=$eclipseProductId"
          File splashFile = PluginUtils.findPluginSplash(project)
          if(splashFile.exists())
            configWriter.println "osgi.splashLocation=${splashFile.absolutePath}"
          File osgiFrameworkFile = PluginUtils.getOsgiFrameworkFile(project)
          configWriter.println "osgi.framework=file\\:${osgiFrameworkFile.absolutePath}"
          configWriter.println 'osgi.bundles.defaultStartLevel=4'
          configWriter.println 'osgi.bundles=' + bundleLaunchList.values().join(',\\\n  ')
        }

        project.copy {
          from project.configurations.runtime.findAll { it.name.startsWith(PluginUtils.equinoxLauncherPluginName) }
          into runPluginsDir
          // need to rename them to ensure that platform-specific launcher fragments are automatically found
          rename PluginUtils.eclipsePluginMask, '$1_$2'
        }
      }
    } // task prepareRunConfig

    List programArgs = [
      '-configuration',
      runConfigDir,
      '-data',
      runDir,
      '-consoleLog'
    ]

    if(PluginUtils.findPluginSplash(project))
      programArgs.add '-showSplash'

    programArgs.addAll project.run.args

    if(project.run.language) {
      programArgs.add '-nl'
      programArgs.add project.run.language
    }

    project.tasks.run {
      dependsOn project.tasks.prepareRunConfig
      File equinoxLauncherFile = PluginUtils.getEquinoxLauncherFile(project)
      classpath = project.files(new File(runPluginsDir, equinoxLauncherFile.name.replaceAll(PluginUtils.eclipsePluginMask, '$1_$2')))
      main = 'org.eclipse.equinox.launcher.Main'
      args = programArgs
    }

    project.tasks.debug {
      dependsOn project.tasks.prepareRunConfig
      File equinoxLauncherFile = PluginUtils.getEquinoxLauncherFile(project)
      classpath = project.files(new File(runPluginsDir, equinoxLauncherFile.name.replaceAll(PluginUtils.eclipsePluginMask, '$1_$2')))
      main = 'org.eclipse.equinox.launcher.Main'
      args = programArgs
      debug = true
    }
  }

  private void configureTask_wrapLibs() {

    project.task('wrapLibs') {
      group = 'wuff'
      description = 'wraps non-OSGi libraries as OSGi-bundles'
      inputs.files { project.configurations.runtime }
      outputs.dir { PluginUtils.getWrappedLibsDir(project) }
      doLast {
        inputs.files.each { lib ->
          def wrapper = new LibWrapper(project, lib, effectiveConfig.wrappedLibs)
          wrapper.wrap()
        }
      }
    } // task wrapLibs
  } // configureTask_wrapLibs

  @Override
  protected void configureTasks() {
    super.configureTasks()
    configureTask_wrapLibs()
    configureRunTasks()
  }

  @Override
  protected void createExtensions() {
    super.createExtensions()
    project.extensions.create('run', RunExtension)
    project.extensions.create('products', EquinoxAppProductsExtension)
  }

  @Override
  protected String getScaffoldResourceDir() {
    'scaffold/eclipse-equinox-app/'
  }

  @Override
  protected List<String> getModules() {
    super.getModules() + [ 'equinoxApp' ]
  }

  protected String getProductConfigPrefix() {
    'product_equinox_'
  }

  @Override
  protected void populatePluginXml(MarkupBuilder pluginXml, Node existingPluginXml) {
    if(!existingPluginXml?.extension.find({ it.'@point' == 'org.eclipse.core.runtime.applications' })) {
      String appClass = PluginUtils.findClassInSources(project, '**/*Application.groovy', '**/*Application.java')
      if(appClass)
        pluginXml.extension(id: 'application', point: 'org.eclipse.core.runtime.applications') {
          application {
            run class: appClass
          }
        }
    }
  }
}

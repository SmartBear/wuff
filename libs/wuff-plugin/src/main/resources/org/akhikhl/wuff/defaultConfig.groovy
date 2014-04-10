wuff {

  defaultEclipseVersion = '4.3'

  eclipseVersion('4.3') {

    eclipseMavenGroup = 'eclipse-kepler'

    swtlib {
      postConfigure { project ->
        project.dependencies {
          compile "${eclipseMavenGroup}:org.eclipse.jface:+"
          compile "${eclipseMavenGroup}:org.eclipse.swt:+"
          compile "${eclipseMavenGroup}:org.eclipse.swt.${current_os_suffix}.${current_arch_suffix}:+"
        }
      }
    }

    swtapp {
      configure { project ->

        supported_oses.each { platform ->
          supported_archs.each { arch ->

            String productConfigName = "product_swtapp_${platform}_${arch}"
            project.configurations.create(productConfigName)

            supported_languages.each { language ->

              String localizedProductConfigName = "product_swtapp_${platform}_${arch}_${language}"
              def localizedConfig = project.configurations.create(localizedProductConfigName)
              localizedConfig.extendsFrom project.configurations[productConfigName]
            }
          }
        }
      }

      postConfigure { project ->

        project.dependencies {
          compile "${eclipseMavenGroup}:org.eclipse.swt:+"
          compile "${eclipseMavenGroup}:org.eclipse.jface:+"
        }

        supported_oses.each { platform ->
          supported_archs.each { arch ->

            String productConfigName = "product_swtapp_${platform}_${arch}"
            project.dependencies.add productConfigName, "${eclipseMavenGroup}:org.eclipse.swt.${map_os_to_suffix[platform]}.${map_arch_to_suffix[arch]}:+"

            supported_languages.each { language ->

              String localizedProductConfigName = "product_swtapp_${platform}_${arch}_${language}"
              project.dependencies.add localizedProductConfigName, "${eclipseMavenGroup}:org.eclipse.jface.nl_${language}:+"
            }
          }
        }
      }
    }

    osgiBundle {

      postConfigure { project ->

        project.dependencies {
          compile "${eclipseMavenGroup}:org.eclipse.osgi:+"
        }
      }
    }

    eclipseBundle {

      postConfigure { project ->

        project.dependencies {
          compile "${eclipseMavenGroup}:javax.annotation:+"
          compile "${eclipseMavenGroup}:javax.inject:+"
          compile "${eclipseMavenGroup}:org.eclipse.jface:+"
          compile "${eclipseMavenGroup}:org.eclipse.swt:+"
          compile "${eclipseMavenGroup}:org.eclipse.swt.${current_os_suffix}.${current_arch_suffix}:+"
          compile "${eclipseMavenGroup}:org.eclipse.ui:+"
          compile "${eclipseMavenGroup}:org.eclipse.osgi:+"
        }

        project.tasks.jar.manifest {
          instruction 'Require-Bundle', 'org.eclipse.jface'
          instruction 'Require-Bundle', 'org.eclipse.swt'
          instruction 'Require-Bundle', 'org.eclipse.ui'
        }
      }
    }

    equinoxApp {

      configure { project ->

        supported_oses.each { platform ->
          supported_archs.each { arch ->

            String productConfigName = "product_equinox_${platform}_${arch}"
            project.configurations.create(productConfigName)

            supported_languages.each { language ->
              def localizedConfig = project.configurations.create("product_equinox_${platform}_${arch}_${language}")
              localizedConfig.extendsFrom project.configurations[productConfigName]
            }
          }
        }
      } // configure

      postConfigure { project ->

        project.dependencies {
          compile "${eclipseMavenGroup}:org.eclipse.core.runtime:+"
          runtime "${eclipseMavenGroup}:org.eclipse.core.runtime.compatibility.registry:+"
          compile "${eclipseMavenGroup}:org.eclipse.equinox.app:+"
          runtime "${eclipseMavenGroup}:org.eclipse.equinox.ds:+"
          runtime "${eclipseMavenGroup}:org.eclipse.equinox.event:+"
          runtime "${eclipseMavenGroup}:org.eclipse.equinox.launcher:+"
          runtime "${eclipseMavenGroup}:org.eclipse.equinox.launcher.${current_os_suffix}.${current_arch_suffix}:+"
          runtime "${eclipseMavenGroup}:org.eclipse.equinox.util:+"
          compile "${eclipseMavenGroup}:org.eclipse.osgi:+"
          runtime "${eclipseMavenGroup}:org.eclipse.osgi.services:+"
          runtime "${eclipseMavenGroup}:com.ibm.icu:+"
          runtime "${eclipseMavenGroup}:javax.xml:+"
        }

        supported_oses.each { platform ->
          supported_archs.each { arch ->

            String productConfigName = "product_equinox_${platform}_${arch}"
            project.dependencies.add productConfigName, "${eclipseMavenGroup}:org.eclipse.equinox.launcher.${map_os_to_suffix[platform]}.${map_arch_to_suffix[arch]}:+"

            supported_languages.each { language ->
              String localizedProductConfigName = "product_equinox_${platform}_${arch}_${language}"
              project.dependencies.add localizedProductConfigName, "${eclipseMavenGroup}:org.eclipse.equinox.launcher.${current_os_suffix}.${current_arch_suffix}.nl_${language}:+"
              project.dependencies.add localizedProductConfigName, "${eclipseMavenGroup}:org.eclipse.osgi.nl_${language}:+"
              project.dependencies.add localizedProductConfigName, "${eclipseMavenGroup}:org.eclipse.osgi.services.nl_${language}:+"
            }
          }
        }

        project.tasks.jar.manifest {
          instruction 'Require-Bundle', 'org.eclipse.core.runtime'
        }
      }
    }
  }

  wrappedLibs {
    /^ant-optional/ {
      excludeImport(/^COM\.ibm\.netrexx\.process$/)
    }

    /^commons-logging/ {
      excludeImport(/^org\.apache\.log/)
      excludeImport(/^org\.apache\.avalon\.framework\.logger/)
    }

    /^avalon-framework/ {
      excludeImport(/^org\.apache\.log/)
      excludeImport(/^org\.apache\.avalon\.framework\.parameters/)
    }

    /^batik-js$/ {
      excludeImport(/^org\.apache\.xmlbeans$/)
    }

    /^batik-script$/ {
      excludeImport(/^org\.mozilla\.javascript$/)
    }

    /^fop$/ {
      excludeImport(/^javax\.media\.jai$/)
      excludeImport(/^org\.apache\.tools\.ant/)
    }

    /^jaxb-impl/ {
      excludeImport(/^com\.sun\.xml\.fastinfoset/)
      excludeImport(/^org\.jvnet\.fastinfoset$/)
      excludeImport(/^org\.jvnet\.staxex/)
    }

    /^jdom$|^jdom-b8$/ {
      excludeImport(/^oracle\.xml\.parser$/)
      excludeImport(/^oracle\.xml\.parser\.v2$/)
      excludeImport(/^org\.apache\.xerces\.dom$/)
      excludeImport(/^org\.apache\.xerces\.parsers$/)
      excludeImport(/^org\.jaxen\.jdom$/)
      excludeImport(/^org\.jaxen$/)
    }

    /^jdom2$/ {
      excludeImport(/^oracle\.xml\.parser$/)
      excludeImport(/^oracle\.xml\.parser\.v2$/)
      excludeImport(/^org\.apache\.xerces\.dom$/)
      excludeImport(/^org\.apache\.xerces\.parsers$/)
    }

    /^ojdbc/ {
      excludeImport(/^javax\.resource$/)
      excludeImport(/^javax\.resource\.spi$/)
      excludeImport(/^javax\.resource\.spi\.endpoint$/)
      excludeImport(/^javax\.resource\.spi\.security$/)
      excludeImport(/^oracle\.i18n\.text\.converter$/)
      excludeImport(/^oracle\.ons$/)
      excludeImport(/^oracle\.security\.pki$/)
    }

    /^saxon/ {
      excludeImport(/^com\.saxonica\.validate$/)
    }

    /^svnkit/ {
      excludeImport(/^org\.tmatesoft\.sqljet/)
      excludeImport(/^org.tigris.subversion.javahl$/)
    }

    /^xalan$/ {
      excludeImport(/^sun\.io$/)
    }

    /^xmlgraphics-commons$/ {
      excludeImport(/^com\.sun\.image\.codec/)
    }

    /^jaxen$/ {
      excludeImport(/^nu.xom$/)
      excludeImport(/^org\.jdom/)
      excludeImport(/^org\.dom4j/)
    }

    /^xercesImpl$/ {
      excludeImport(/^sun.io$/)
    }
  }
}

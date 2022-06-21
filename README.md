JBang / Eclipse integration POC
===============================

This experimental plugin aims to let you author [JBang](https://github.com/jbangdev/jbang) scripts in Eclipse with ease.

##Features:

- Import JBang Scripts as projects
- Synchronize JBang Dependencies to existing projects
- Validation of JBang scripts on file save
- JBang runtime management

Cuurently supports 
- `//DEPS` / `@Grab` dependencies
- `//JAVA` version
- `//SOURCES` additional sources 

By default, the plugin will try to find JBang from the PATH, from `~/.jbang/bin` or `~/.sdkman/candidates/jbang/current/bin/jbang`. Alternate locations can be defined in `Preferences` > `JBang` > `Installations`


## Import JBang Script as project

Import JBang scripts in Eclipse: Import Project... > JBang > JBang script.

<img width="552" alt="import-jbang" src="https://user-images.githubusercontent.com/148698/174771869-39400429-5a42-4257-97cf-2fb682432b25.png">

A project named after the script file will be created in the current workspace, that will link to the real script file.

## Synchronize JBang

The `Synchronize JBang` command is available when right-clicking on a JBang script in an existing project. If the script belongs to a non-Java project, the Java nature will be added to that project. 
The source folder containing the JBang file will automatically be added to the project's classpath. The `JBang Dependencies` classpath container will be added to the classpath as well.

![Jun-20-2022 19-32-10](https://user-images.githubusercontent.com/148698/174653463-39ea956f-cba8-40d7-8cbf-0d3b50cf49c1.gif)


*Caveats*:
- JBang files and dependencies will leak to the project's main and test-scoped sources
- the project's main files and dependencies will leak to the JBang script
- If the script file is at the root of the project and there are other source folders nested beneath that root, compilation errors will occur in those folders
- Other build tools (m2e/buildship) might conflict with the classpath changes. Typically, Updating the Maven project configuration would remove the JBang source folder and its classpath container. Re-running `Synchronize JBang` will be necessary to get completion and validation in JBang scripts.


Installation
------------

In Eclipse:

- open Help > Install New Software...
- work with: `https://jbangdev.github.io/jbang-eclipse/update/`
- expand the category and select the Jbang Eclipse Feature
- proceed with the installation
- restart Eclipse


Build
-----

Open a terminal and execute:

    ./mvnw clean package
    
You can then install the generated update site from `dev.jbang.eclipse.site/target/repository`, also zipped as `dev.jbang.eclipse.site-<VERSION>-SNAPSHOT.zip`

License
-------
EPL 2.0, See [LICENSE](LICENSE) file.


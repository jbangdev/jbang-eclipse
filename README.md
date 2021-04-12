J'Bang! / Eclipse integration POC
===============================

Import [J'Bang!](https://github.com/jbangdev/jbang) scripts in Eclipse: Import Project... > JBang > JBang script

Currently expects to use JBang from `~/.sdkman/candidates/jbang/current/`. Yes it sucks, I know.

Cuurently supports 
- `//DEPS` / `@Grab` dependencies
- `//JAVA` version
- `//SOURCES` additional sources 

Installation
------------

- go to https://github.com/fbricon/jbang.eclipse/actions?query=is%3Asuccess+branch%3Amaster++
- click on the last successful build
- download the jbang.eclipse.zip archive

In Eclipse,
- open Help > Install New Software...
- click Add...
- click Archive...
- select the jbang.eclipse.zip archive, click Add
- expand the category and select the Jbang Eclipse Feature
- proceed with the installation
- restart Eclipse


Build
-----

Open a terminal and execute:

    ./mvnw clean package
    
You can then install the generated update site from `jbang.eclipse.site/target/jbang.eclipse.site-<VERSION>-SNAPSHOT.zip`

License
-------
EPL 2.0, See [LICENSE](LICENSE) file.


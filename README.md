J'Bang! / Eclipse integration POC
===============================

Import [JBang](https://github.com/jbangdev/jbang) scripts in Eclipse: Import Project... > JBang > JBang script

Currently expects to use JBang from `~/.sdkman/candidates/jbang/current/`. Yes it sucks, I know.

Cuurently supports 
- `//DEPS` / `@Grab` dependencies
- `//JAVA` version
- `//SOURCES` additional sources 

Installation
------------

In Eclipse:

- open Help > Install New Software...
- work with: `http://fbricon.github.io/jbang.eclipse/update/`
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

# gpom-maven-plugin
  **Originating from [stackoverflow#76330346](https://stackoverflow.com/questions/76330346/the-problem-of-maven-multi-layer-dependency-transfer), this plugin was built.**  
  **Generates a pom file based on the specified dependencies.**  

## Dependency  
```xml
<plugin>
    <groupId>io.github.kerwin612</groupId>
    <artifactId>gpom-maven-plugin</artifactId>
</plugin>
```

## Example

**internal.lib1**
> Internal lib 1, which references a third-party library dependency-A of the central repository
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <version>1.0</version>
    <groupId>com.example</groupId>
    <artifactId>internal.lib1</artifactId>

    <dependencies>
        <dependency>
            <version>1.0</version>
            <groupId>com.external</groupId>
            <artifactId>dependency-A</artifactId>
        </dependency>
    </dependencies>

</project>
``` 

**internal.lib2**
> Internal lib 2, which references a third-party library dependency-B of the central repository
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <version>1.0</version>
    <groupId>com.example</groupId>
    <artifactId>internal.lib2</artifactId>

    <dependencies>
        <dependency>
            <version>1.0</version>
            <groupId>com.external</groupId>
            <artifactId>dependency-B</artifactId>
        </dependency>
    </dependencies>

</project>
``` 

**internal.lib3**:  
> Internal lib3, which references a third-party library dependency-C of the central repository and internal.lib1, internal.lib2  

**To make lib3 available for external use, but lib1 and lib2 cannot be published to a central repository.**  
**Therefore, the output lib3 package needs to contain lib1 and lib2, and merge their dependencies into the pom of lib3.**  
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <version>1.0</version>
    <groupId>com.example</groupId>
    <artifactId>internal.lib3</artifactId>

    <dependencies>
        <dependency>
            <version>1.0</version>
            <groupId>com.example</groupId>
            <artifactId>internal.lib2</artifactId>
        </dependency>
        <dependency>
            <version>1.0</version>
            <groupId>com.example</groupId>
            <artifactId>internal.lib3</artifactId>
        </dependency>
        <dependency>
            <version>1.0</version>
            <groupId>com.external</groupId>
            <artifactId>dependency-C</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <!--unpack lib1, and lib2 to lib3 classes-->
                    <execution>
                        <id>unpack-internal-dependencies</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>unpack-dependencies</goal>
                        </goals>
                        <configuration>
                            <includeGroupIds>com.example</includeGroupIds>
                            <excludes>**/META-INF/maven/**</excludes>
                            <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                        </configuration>
                    </execution>
                    <!--output all dependencies of lib3 (including the dependency list of lib1 and lib2, but excluding lib1 and lib2 themselves) to the specified file-->
                    <execution>
                        <id>list-dependencies</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>list</goal>
                        </goals>
                        <configuration>
                            <excludeGroupIds>com.example</excludeGroupIds>
                            <outputFile>${project.build.directory}/dependencies</outputFile>
                            <silent>true</silent>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>io.github.kerwin612</groupId>
                <artifactId>gpom-maven-plugin</artifactId>
                <version>0.0.1</version>
                <executions>
                    <!--generate a new pom file based on the dependency file-->
                    <execution>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <dependenciesPath>${project.build.directory}/dependencies</dependenciesPath>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <id>default-jar</id>
                        <phase>none</phase>
                    </execution>
                    <execution>
                        <id>package-jar</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                        <phase>package</phase>
                        <configuration>
                            <archive>
                                <!--use a specific pom file instead of the original pom file-->
                                <addMavenDescriptor>false</addMavenDescriptor>
                            </archive>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```  

**customer.service**  
> Add lib3 dependency and successfully pass all dependencies required by lib1, lib2, lib3

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <version>1.0</version>
    <groupId>com.customer</groupId>
    <artifactId>customer.service</artifactId>

    <dependencies>
        <!--mvn org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file -Dfile=./com.example.internal.lib3.1.0.jar-->
        <dependency>
            <version>1.0</version>
            <groupId>com.example</groupId>
            <artifactId>internal.lib3</artifactId>
        </dependency>
        <dependency>
            <version>1.0</version>
            <groupId>com.external</groupId>
            <artifactId>dependency-D</artifactId>
        </dependency>
    </dependencies>

</project>
```
**The dependency tree for `customer.service` is as follows:**
```
customer.service
    -> internal.lib3
        -> dependency-A
        -> dependency-B
        -> dependency-C
    -> dependency-D
```

# Configuration

| **name** | **description**                                                                                                                                                                       | **required** | **default value**                                                                                    |
| --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------:|------------------------------------------------------------------------------------------------------|
| `dependenciesPath` | The path to the file containing the dependencies, The file content refers to the output of [dependency:list](https://maven.apache.org/plugins/maven-dependency-plugin/list-mojo.html) |      Y       | `none`                                                                                               |
| `outputPath` | The path to pom file output.                                                                                                                                                          |      N       | **${project.build.outputDirectory}/META-INF/maven/${project.groupId}/${project.artifactId}/pom.xml** |

package org.kerwin612.gpom;

import com.google.common.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(
    name = "generate",
    threadSafe = true,
    defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GPomMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(required = true)
    private File dependenciesPath;

    @Parameter
    private File outputPath;

    public void execute() throws MojoExecutionException {
        if (dependenciesPath == null || !dependenciesPath.exists()) {
            throw new MojoExecutionException("dependencies file invalid.");
        }
        try {
            if (outputPath == null) {
                outputPath = Paths.get(project.getBuild().getOutputDirectory()).resolve("META-INF").resolve("maven").resolve(project.getGroupId()).resolve(project.getArtifactId()).resolve("pom.xml").toFile();
            }
            Files.createParentDirs(outputPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create parent dirs.");
        }

        Pattern pattern = Pattern.compile("(?<groupId>[^:]+):(?<artifactId>[^:]+):jar:((?<classifier>.*):)?(?<version>[\\d\\.-]+):(?<scope>[^:]+)");
        try {
            StringBuffer dependenciesString = new StringBuffer();
            java.nio.file.Files.lines(dependenciesPath.toPath()).forEach(s -> {
                Matcher matcher = pattern.matcher(s.trim());
                if (!matcher.find()) return;
                try {
                    dependenciesString.append(String.format("<dependency>\n" +
                        "    <groupId>%s</groupId>\n" +
                        "    <artifactId>%s</artifactId>\n" +
                        "    <version>%s</version>\n" +
                        "    <scope>%s</scope>\n" +
                        "    <classifier>%s</classifier>\n" +
                        "</dependency>", matcher.group("groupId"), matcher.group("artifactId"), matcher.group("version"), matcher.group("scope"), Objects.toString(matcher.group("classifier"), "")));
                } catch (Exception e) {
                    getLog().error(e);
                }
            });
            Files.write(String.format("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "<name>%s</name>\n" +
                "<groupId>%s</groupId>\n" +
                "<artifactId>%s</artifactId>\n" +
                "<version>%s</version>\n" +
                "\n" +
                "<dependencies>\n" +
                "%s\n" +
                "</dependencies>\n" +
                "\n" +
                "</project>\n", project.getName(), project.getGroupId(), project.getArtifactId(), project.getVersion(), dependenciesString), outputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create parent dirs.");
        }

        getLog().info(String.format("generate pom with <%s> to <%s>", dependenciesPath, outputPath));
    }
}

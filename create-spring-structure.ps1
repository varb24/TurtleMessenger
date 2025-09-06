# Prompt for input
$basePackage = Read-Host "Enter base package (e.g. com.example.turtlemessenger)"
$appName = Read-Host "Enter application name (e.g. TurtleMessenger)"

# Normalize inputs
$packagePath = $basePackage -replace '\.', '/'
$appClassName = "$appName" + "Application"
$artifactId = $appName -replace '\s', '' | ForEach-Object { $_.ToLower() }
$groupId = $basePackage.Substring(0, $basePackage.LastIndexOf('.'))

# Base paths
$srcBase = "src/main/java"
$resourcesBase = "src/main/resources"
$testBase = "src/test/java"

# Folder structure
$folders = @(
    "$srcBase/$packagePath/config",
    "$srcBase/$packagePath/controller",
    "$srcBase/$packagePath/dto",
    "$srcBase/$packagePath/exception",
    "$srcBase/$packagePath/model",
    "$srcBase/$packagePath/repository",
    "$srcBase/$packagePath/service/impl",
    "$srcBase/$packagePath/util",
    "$testBase/$packagePath",
    "$resourcesBase/static",
    "$resourcesBase/templates"
)

foreach ($folder in $folders) {
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    Write-Host "Created: $folder"
}

# Create main application class
$appClassPath = "$srcBase/$packagePath/$appClassName.java"
$appClassContent = @"
package $basePackage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $appClassName {
    public static void main(String[] args) {
        SpringApplication.run($appClassName.class, args);
    }
}
"@
$appClassContent | Set-Content -Path $appClassPath
Write-Host "Created main application class: $appClassPath"

# Create test class
$testClassPath = "$testBase/$packagePath/${appClassName}Tests.java"
$testClassContent = @"
package $basePackage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ${appClassName}Tests {

    @Test
    void contextLoads() {
        // Application context loads
    }
}
"@
$testClassContent | Set-Content -Path $testClassPath
Write-Host "Created test class: $testClassPath"

# Prompt for build tool
$buildTool = Read-Host "Choose build tool (maven/gradle)"

if ($buildTool -eq "maven") {
    $pomContent = @"
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
           http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>$groupId</groupId>
  <artifactId>$artifactId</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>

  <name>$appName</name>
  <description>Spring Boot Starter Project</description>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
    <relativePath/>
  </parent>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>

</project>
"@
    $pomContent | Set-Content -Path "pom.xml"
    Write-Host "Created pom.xml"

} elseif ($buildTool -eq "gradle") {
    $gradleContent = @"
plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
}

group = "$groupId"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
"@
    $gradleContent | Set-Content -Path "build.gradle.kts"
    Write-Host "Created build.gradle.kts"
    Write-Host "⚠️  Note: Run 'gradle wrapper' to create wrapper scripts."

} else {
    Write-Host "❌ Invalid build tool. Skipping build file creation."
}

## Trigon - Backend

___ 

### Technologies
Java 17

### Tools and Setup




#### Jib

Below are the jib commands to create docker image and also pushes to Artifact Repository.
##### Test
```sh
./gradlew -Djib.useOnlyProjectCache=true -PappEnvironment=test jib -Djib.to.image='europe-west1-docker.pkg.dev/bmg-sap-royalty-post-test/trigon/masterdata:v1.0'

##### Make sure you have repo read/write access for  https://bitbucket.org/bmgpipeline/trigon-mt-gke.git
-- Update above image tag version in overlays/test/versions/masterdata.version
-- commit and push the code
```

##### Staging
```sh
./gradlew -Djib.useOnlyProjectCache=true -PappEnvironment=uat jib -Djib.to.image='europe-west1-docker.pkg.dev/bmg-sap-royalty-post-staging/trigon/masterdata:v1.36.UAT'

##### Make sure you have repo read/write access for  https://bitbucket.org/bmgpipeline/trigon-mt-gke.git
-- Update above image tag version in overlays/stage/versions/masterdata.version
-- commit and push the code
```

##### Production
```sh
./gradlew -Djib.useOnlyProjectCache=true -PappEnvironment=prod jib -Djib.to.image='europe-west1-docker.pkg.dev/bmg-sap-royalty-post-prod/trigon/masterdata:v1.0'

##### Make sure you have repo read/write access for  https://bitbucket.org/bmgpipeline/trigon-mt-gke.git
-- Update above image tag version in overlays/prod/versions/masterdata.version
-- commit and push the code
```


## Static code analysis

## Spot Bugs
https://github.com/spotbugs/spotbugs-gradle-plugin
For now, only High bugs enabled to check.
```
./gradlew check

```

### Spotless Java

Spotless is a Gradle plugin for Java formatting plugin.

As of now, `spotlessCheck` is dependency task with `build` task to check formatting violations.

To fix formatting violations, spotless can check and apply formatting on the fly with simple gradle command.

```sh
./gradlew spotlessApply
```

## Dependency Check
[Dependency-Check](dependency_check) is a Software Composition Analysis (SCA) tool that attempts to detect publicly disclosed vulnerabilities contained within a project's dependencies.

[dependency_check]: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html

first time this task is executed it may take 5-20 minutes as it downloads and processes the data from the National Vulnerability Database (NVD) hosted by NIST: https://nvd.nist.gov

```
./gradlew dependencyCheckAnalyze
```

## jacocoTestReport
This Code Coverage report will be generated at the time of gradle build.

## SonarQube
## SonarQube Configuration
- I am working on Windows environment with SonarQube.
- Download SonarQube from https://www.sonarqube.org/
- Install SonarQube. Simply unzip the zip folder to any drive.
- Open command prompt and navigate to the directory <physical drive>:\sonarqube-8.4.0.35506\bin\windows-x86-64
- Now execute the batch file StartSonar.bat
- Wait for few minutes to start-up the SonarQube until you see something like below in the console:
  ```
  2023.07.20 21:41:01 INFO  app[][o.s.a.SchedulerImpl] Process[ce] is up
  2023.07.20 21:41:01 INFO  app[][o.s.a.SchedulerImpl] SonarQube is operational
  ```
- Now hit the URL http://localhost:9000/ in the browser. You will see no project in the dashboard:
- Once sonarqube is up and running execute below command.
    ```
    ./gradlew sonarqube
    ```
- Note: If you want to change the properties of sonarqube, check below configuration in build.gralde
   ```
  sonarqube {
    properties {
        property "sonar.coverage.jacoco.xmlReportPath", "${project.buildDir}/reports/jacoco.xml"
        property "sonar.junit.reportPaths", "${project.buildDir}/reports/jacoco.xml"
        property "sonar.verbose", true
        property "sonar.sources", "src/main"
        property "sonar.tests", "src/test"
        property "sonar.host.url", "http://localhost:9000"
        property "sonar.projectName", "Trigon"
        property "sonar.projectKey", "com.bmg.trigon"
        property 'sonar.login', 'admin'
        property 'sonar.password', 'Admin@1234'
    }
  }
   ```

## Pre-commit Hook

### Setup
Follow the below steps to set up pre-commit hook.

- Create a folder named `scripts` in the root of the project.
- Create a file named `pre-commit` in the `scripts` folder.
- Copy the following content and paste it in the `pre-commit` file.
```
#!/bin/bash

echo "Running git pre-commit hook"

./gradlew clean build

RESULT=$?

# return 1 exit code if running checks fails
[ $RESULT -ne 0 ] && exit 1
exit 0
```
- Make sure the file has execute permission. If not, run `chmod +x pre-commit` command from the `scripts` folder.
- Create a gradle task named `installGitHook` in `build.gradle` file, by adding the following content in the `build.gradle` file.
```
task installGitHook(type: Copy) {
	System.out.println("Installing git hook..")
	from new File(projectDir, 'scripts/pre-commit')
	into { new File(projectDir, '.git/hooks') }
	fileMode 0777
}

assemble.dependsOn installGitHook
```

### Enable
To enable pre-commit hook, run below command from root of the project.
```
./gradlew installGitHook
```

- First time this task is executed it will copy the `scripts/pre-commit` file to `.git/hooks/pre-commit`
- This will ensure that before every commit, the code is formatted and checked for any violations.
- If there are any violations, the commit will fail and the developer will have to fix the violations and commit again.
- If there are no violations, the commit will be successful.
- To know the list of tasks that are executed before every commit, please refer to `scripts/pre-commit` file.
- Ideally to check the list of tasks that `./gradlew clean build` runs, please execute this command `./gradlew clean build --console=plain`

# About 'Commons' module dependency

## Description

This repository should utilizes a dependency from the "common" repository.

## Setup Instructions

To include the dependency from the "common" repository, follow these steps:

1. **Add Repository**: Add the "common" repository to the list of repositories in the `build.gradle` file of this project. Ensure that the `url` points to the location of the "common" repository where the JAR is published.

    ```groovy
    repositories {
        mavenCentral()
        maven {
            url = uri('../gradle-repo')
        }
    }
    ```

2. **Add Dependency**: Add the dependency using the notation below. Make sure to specify the correct group, artifact, and version according to the JAR published in the "common" repository.

    ```groovy
    implementation "com.bmg.trigon.common:bmg-trigon-common:0.0.1", {
        changing = true
    }
    ```

   Ensure that `0.0.1` matches the version of the JAR published in the "common" repository.

3. **Sync Gradle**: Sync the Gradle project to download the dependency and update the project configuration.

4. **Usage**: You can now use the classes and functionalities provided by the "common" repository in your project.

## Additional Notes

- Ensure that the "common" repository is published and the JAR is available before adding it as a dependency.
- If you encounter any issues related to dependency resolution or version conflicts, coordinate with the team maintaining the "common" repository.
- Make sure to keep the `build.gradle` file updated with the latest version of the dependency from the "common" repository.
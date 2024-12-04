# Deliver Rebuild
Read this README thoroughly before starting the project, and make sure to follow the mentioned guidelines.

## Technical Specifications
- Java 17
- Rabbitmq 3.12.2
- Spring Boot 3.2.3
- PostgresSQL

## Local setup
- Install RabbitMQ and create required Queues and update required configurations in `application-local.yml`.
- Update `application-local.yml` with your local configuration.
- Add Master or Worker role in environment variable.
- Run the application with below VM argument.
    ```
  -Dspring.profiles.active=local
    ```
fadfs
## Run as Master or Worker
- To run only as Master application, add `MASTER_ROLE=master`in environment variable.
- To run only as Worker application, add `WORKER_ROLE=worker`in environment variable.
- Based on the role, the application will start either as Master or Worker.
- To run both Master and Worker, add `MASTER_ROLE=master;WORKER_ROLE=worker` in environment variable.
  
## Gradle Tasks/Commands

### Spotless
- Spotless is used to format code according to predefined rules and configurations.
- A gradle task `spotlessapply` can be found in gradle tasks. Execute that task to apply the formatting.
- To apply the formatting from command line, run the following command:
    ```
    ./gradlew spotlessapply
    ```
### Check Style
- Checkstyle is used to scan code for style violations based on predefined rules.
- A gradle task `checkstyleMain` and `checkstyleTest` can be found in gradle tasks. Execute those tasks to run Checkstyle for main and test classes respectively.
- To run Checkstyle from command line, run the following command:
   #### main
   ```
   ./gradlew checkstyleMain
   ```
   #### test
   ```
   ./gradlew checkstyleTest
   ```

### PMD
- PMD is a static code analysis tool that checks code for potential problems.
- PMD is installed during the build process.
- Checkout `scripts/pmd` for the rulesets defined for PMD.
- To learn more about PMD, visit [PMD website](https://pmd.github.io/) and to learn about the rulesets, visit [PMD rulesets](https://pmd.github.io/latest/pmd_rules_java.html).
- `pmdMain` and `pmdTest` can be found in gradle tasks. Execute those tasks to run PMD for main and test classes respectively.
- To run PMD from command line, run the following command:
    ```
    ./gradlew pmdMain
    ```
- To run PMD tests from command line, run the following command:
    ```
    ./gradlew pmdTest
    ```
- Once the PMD checks are ran, reports are generated in `build/reports/pmd` directory. Check the `index.html` file for the report. Make sure to fix the issues reported by PMD.

## Pre-commit 
- Pre-commit is a tool that runs checks before committing code.
- Pre-commit hooks run whenever you commit your changes, if any of the checks fail, the commit is aborted.
- Check `scripts/pre-commit` for the pre-commit hook script.
- Make sure to re-install whenever you update the pre-commit hook script.
- Execute gradle task `installLocalGitHook` to install pre-commit hooks/update the pre-commit hooks after updating the script.
- To bypass pre-commit hooks check for changes, use the `--no-verify` option for the commit command.

## Rabbitmq setup
- Install docker CLI.
- Pull docker image `rabbitmq:3.12.2-management`.
   ```
   docker pull 
   ```
- Run docker image
   ```
   docker run -d --name rabbitmq -p 15672:15672 -p 5672:5672 rabbitmq:3.12.2-management
   ```
## Sonarqube setup
- Make sure you have Sonarqube installed.
- Update sonarqube.gradle with your sonarqube configuration.
- Run sonarqube with below command.
  ```
  ./gradlew sonar
  ```
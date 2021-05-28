# FDA-User Reg WS

This project consists of APIs that required for MyStudies User registration.This project is developed in Spring MVC framework on Labkey environment.

## Evaluation Setup Instructions

This module can be used to create a standalone distribution of the MyStudies User Registration Server.
_(The following commands and paths are relative to your `UserReg-WS` enlistment)_

1. Install JDK 14+
   - [AdoptOpenJDK](https://adoptopenjdk.net/releases.html?variant=openjdk14&jvmVariant=hotspot)
   - Your `JAVA_HOME` environment variable should point at a compatible JDK install
1. Create Registration LabKey distribution
   - (Linux/MacOS) `./gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
   - (Windows) `.\gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
1. Locate distribution archive
    - (Linux/MacOS) `dist/Registration/LabKey*-Registration.tar.gz`
    - (Windows) `dist\Registration\LabKey*-Registration.tar.gz`
1. Follow [instructions for manual deployment](https://www.labkey.org/Documentation/wiki-page.view?name=manualInstall) of the distribution archive

## Developer Setup Instructions
- [LabKey Developer Setup](https://www.labkey.org/Documentation/wiki-page.view?name=devMachine)
- [FDA MyStudies: User-Reg WS](https://www.labkey.org/FDAMyStudiesHelp/wiki-page.view?name=setupInstructions#userReg)

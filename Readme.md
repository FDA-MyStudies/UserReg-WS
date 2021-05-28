# FDA-User Reg WS

This project consists of APIs that required for MyStudies User registration.This project is developed in Spring MVC framework on Labkey environment.

## Evaluation Setup Instructions

This module can be used to create a distribution of the User Registration Web Server.
_(The following commands and paths are relative to your `UserReg-WS` enlistment)_

1. Create Registration LabKey distribution
   - (Linux/MacOS) `./gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
   - (Windows) `.\gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
1. Locate distribution archive
    - (Linux/MacOS) `dist/Registration/Registration/LabKey*-Registration.tar.gz`
    - (Windows) `dist\Registration\Registration\LabKey*-Registration.tar.gz`
1. Follow [instructions for manual deployment](https://www.labkey.org/Documentation/wiki-page.view?name=manualInstall) of the distribution archive

## Developer Setup Instructions
- [FDA MyStudies: User-Reg WS](https://www.labkey.org/FDAMyStudiesHelp/wiki-page.view?name=setupInstructions#userReg)

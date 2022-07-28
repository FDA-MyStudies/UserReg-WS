# FDA-User Reg WS

This project contains source code for the FDA MyStudies User Registration module. This module is developed and
deployed on the LabKey Server environment.

## Setup Instructions
 
You can build and deploy this module to a standard LabKey Server deployment; see the
[LabKey Developer Setup](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=devMachine)
page for more details.

If you prefer, you can create a standalone distribution of the MyStudies User Registration server using the following steps.
_(Commands and paths are relative to your `UserReg-WS` enlistment)_

1. Install JDK 17
   - Download and install the latest [Eclipse Temurin™ JDK 17](https://adoptium.net/releases.html?variant=openjdk17&amp;jvmVariant=hotspot)
   - Point your `JAVA_HOME` environment variable at this JDK 17 installation
1. Create User Registration LabKey distribution
   - (Linux/MacOS) `./gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
   - (Windows) `.\gradlew -I init.gradle -PdeployMode=prod :distributions:Registration:distribution`
1. Locate distribution archive
    - (Linux/MacOS) `dist/Registration/LabKey*-Registration.tar.gz`
    - (Windows) `dist\Registration\LabKey*-Registration.tar.gz`
1. Follow [instructions for manual deployment](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=manualInstall) of the distribution archive

## Developer Setup Instructions
- [LabKey Developer Setup](https://www.labkey.org/Documentation/22.7/wiki-page.view?name=devMachine)
- [FDA MyStudies: User-Reg WS](https://www.labkey.org/FDAMyStudiesHelp/wiki-page.view?name=setupInstructions#userReg)

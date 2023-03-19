git pull
gradlew.bat simplifyDist
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar build/dist/auth.war

git pull
gradle clean simplifyDist --refresh-dependencies
java -agentlib:jdwp=transport=dt_socket,server=n,address=kaleidox.ddns.net:5005,suspend=y -jar build/dist/auth.war

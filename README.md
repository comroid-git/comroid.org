## Deployment Status

### Websites: ![Websites Deployment Status](https://teamcity.comroid.org/app/rest/builds/buildType:(id:org_comroid_website_deploy)/statusIcon)
### Status Server: ![Status Server Deployment Status](https://teamcity.comroid.org/app/rest/builds/buildType:(id:org_comroid_java_status_server_deploy)/statusIcon)
### Status API: ![Status API Deployment Status](https://teamcity.comroid.org/app/rest/builds/buildType:(id:org_comroid_java_status_server_deploy_api)/statusIcon)

## Space Repositories

### Credentials
```
Username: Anonymous.User
Password: anonymous
```

### [Release Repository](https://comroid.jetbrains.space/packages/maven/release): `https://maven.jetbrains.space/comroid/release`
#### Gradle
```groovy
repositories {
    maven {
        url "https://maven.jetbrains.space/comroid/release"
        credentials.username "Anonymous.User"
        credentials.password "anonymous"
    }
}
```

### [Snapshot Repository](https://comroid.jetbrains.space/packages/maven/snapshot): `https://maven.jetbrains.space/comroid/snapshot`
#### Gradle
```groovy
repositories {
    maven {
        url "https://maven.jetbrains.space/comroid/snapshot"
        credentials.username "Anonymous.User"
        credentials.password "anonymous"
    }
}
```

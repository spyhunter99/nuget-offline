# Nuget Offline

An experimental tool to help cache Nuget repository content locally. This data can then be exposed using
a simple web server, such as Apache HTTPD, IIS, or whatever. This enables building/installing artifacts
on an isolated network.

## Status

Under development, PR's welcome

## Usage

`java -jar NugetCacher-1.0.0-SNAPSHOT-jar-with-dependencies.jar (package name)`

This will download and cache all versions of the specified package and all its transitive dependencies.

At the end of the run, all hosts that the tool downloaded content from will be outputted to the stdout.
This list can then be used for dns forwarding, etc

After running, the cached content will be in the `output` directory. 

### Examples 

`java -jar NugetCacher-1.0.0-SNAPSHOT-jar-with-dependencies.jar nunit`

`java -jar NugetCacher-1.0.0-SNAPSHOT-jar-with-dependencies.jar Microsoft.Web.Services3`

`java -jar NugetCacher-1.0.0-SNAPSHOT-jar-with-dependencies.jar jquery`


### Known nuget/artifact hosts

http://nugetgallery.blob.core.windows.net
https://api.nuget.org

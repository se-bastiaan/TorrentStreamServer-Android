TorrentStreamServer-Android [![Release](https://jitpack.io/v/se-bastiaan/TorrentStreamServer-Android.svg)](https://jitpack.io/#se-bastiaan/TorrentStreamServer-Android)
======

A torrent streamer server library for Android using [TorrentStream-Android](https://github.com/se-bastiaan/torrentstream-android).

Want to add subtitles to your video? The server also gives you this possibility and adds the right DLNA headers, in case you need those.

## How to use

Add Jitpack in your root build.gradle at the end of repositories:
```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Add to your dependencies:

```groovy
dependencies {
    compile "com.github.se-bastiaan:TorrentStreamServer-Android:${TorrentStreamServerVersion}"
}
```


##License

    Copyright 2015-2016 Sébastiaan (github.com/se-bastiaan)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

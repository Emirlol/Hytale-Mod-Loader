# Necesse Mod Loader

Allows Fabric mods to be loaded into Necesse!

Currently very WIP. Game paths (and a bunch of stuff) are hardcoded for my machine as it's a very early version and automating that stuff would've been a waste of time considering that making it work is the priority.

## How to build
1. Import the project into IntelIJ.
2. Run **buildAndCopy**, it will automatically put the mod loader jar and dependencies in the `release` folder.

Alternatively, there are `runClient` and `runServer` tasks that will run the scripts that were placed in the `release` folder.

## Credits
1. This project is mostly a fork of [CosmicReach-Mod-Loader](https://github.com/ForwarD-NerN/CosmicReach-Mod-Loader), this wouldn't be possible if I had nothing to start with.
2. EliteMasterEric for the [original template](https://github.com/EliteMasterEric/HelloWorldFabric)
3. 𝓣𝓱𝓮·𝓦𝓱𝔂·𝓔𝓿𝓮𝓷·𝓗𝓸𝔀 for answering my plethora of questions about Fabric Loader itself.

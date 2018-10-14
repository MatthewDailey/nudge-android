# Nudge

Nudge is an android app that helps you beat phone addiction. It harnesses the addictive power of
apps like Facebook and Instagram and lets you redirect it to a more positive uses of time.

## TODO

- [ ] set up app store listing
- [ ] Block websites as well
- [ ] continuous deployment to app store


## Ownership

PackageArrayAdapter
- allow configuring:
    - type of package list
    - package action and UI for package list
    - onLoad complete trigger for UI

<*>Activity
- configuration of package array adapter

PackageInfoManager
- cache package -> (name, icon)

PackageListManager
- Multiple types,
- List elements representing packages

# Deploy Process

Nudge is deployed via [Fastlane](https://docs.fastlane.tools/getting-started/android/setup/). To set this up,
you need to have some information from 1password set up locally:

1. `~/reactiverobot-keystore` needs
2. `~/.gradle/gradle.properties` needs to include several variables (see [this article](https://stackoverflow.com/questions/18328730/how-to-create-a-release-signed-apk-file-using-gradle))
including a reference to the local keystore above.
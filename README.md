# Nudge

Nudge is an android app that helps you beat phone addiction. It harnesses the addictive power of
apps like Facebook and Instagram and lets you redirect it to a more positive uses of time.

## TODO

- [ ] Stream-line choosing package list manager
- [ ] Allow un-pinning via long-press
- [ ] Show better list of all apps (not just user installed)
- [ ] Improve icon
- [ ] Improve parsing of foreground app.
- [ ] Block websites as well


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
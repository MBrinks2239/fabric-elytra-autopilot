# Elytra AutoPilot

**This mod requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).**
***

This is a fork of TheMegax's implementation of the mod. However, TheMegax quit the project. HumanoidSandvichDispenser and R4z0rX contributed to the 1.20 implementation. Many people were involved, so check the mod author list for a full list of contributors.

Here is the link to TheMegax's mod page: [https://www.curseforge.com/minecraft/mc-mods/elytra-autopilot](https://www.curseforge.com/minecraft/mc-mods/elytra-autopilot).

## How to Use
Press the assigned key (default "R") while flying at a sufficient altitude to enable 'Auto Flight'. In Auto Flight mode, the mod will adjust your pitch between ascending and descending, resulting in a net altitude gain.

To open the config screen and enable Mod Menu, go into the mod menu and open the configuration screen there. 

## Camera Decoupling
Enable **Camera Decoupled** in the config to look around freely while Auto Flight is flying for you.
- During `/flyto` the autopilot owns the heading, so both yaw and pitch are free.
- In plain Auto Flight the mouse still steers your heading, so only pitch is decoupled.

The free camera overrides the camera rotation directly (`Camera.alignWithEntity` → `Camera.setRotation`), so it stays rock solid instead of having to fight Minecraft's per-tick rotation interpolation. Your player rotation is never touched, which means flight control and the server-side rotation are unaffected by free look.

## /flyto Command
**Syntax:** `/flyto X Z` or `/flyto <name>`

While flying, use this command to automatically fly to the specified coordinates. When near the destination, the mod will attempt to slow you down by circling around the target to avoid fall damage. You can disable this at any time by turning off Auto Flight or toggling the setting in the config.

## /takeoff Command
**Syntax:** `/takeoff` or `/takeoff X Z` or `/takeoff <name>`

If you have an Elytra equipped and fireworks in either your main or off-hand, this command will launch you upwards to a configurable height (default: 180 blocks) before activating Auto Flight. If coordinates are provided, it will then use `/flyto` to navigate to the specified location automatically.

## /flylocation Command
**Syntax:** `/flylocation set <name> X Z` or `/flylocation remove <name>`

Use this command to add or remove quick fly locations.

## /land Command
**Syntax:** `/land`

While flying, use this command to force a landing at any time. Useful for quickly returning to the ground!

### Risky Landing
Disabled by default but can be enabled in the config. When active, this setting modifies the landing behavior to a riskier approach, nosediving until the last moment before pulling up. Not recommended for laggy servers or clients!

## Xaero Minimap Support
If you prefer not to use the built-in `/flylocation` command or are already managing waypoints with Xaero Minimap, good news! You can now use `/flyto` and `/takeoff` directly with your Xaero Minimap waypoints.

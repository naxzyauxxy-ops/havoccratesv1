# HavocCrates ❄ Christmas Edition

Donut-style crates for Paper/Spigot **1.20+**, reading the previous plugin's `crates.yml` as-is.

## What is new in 4.0

| Feature | Detail |
| --- | --- |
| 🎁 Mystery present | A present sits in every crate menu. One key opens it and gives **one random reward from that crate**, weighted by `CHANCES` in crates.yml |
| Odds in the lore | `%chances%` expands into one line per reward with its real percentage |
| Christmas theme | Snowy menu, candy-cane border, sleigh-bell buttons, chime on purchase, ❄/❅ messages |
| Candy-cane border | `CONFIRM-MENU.BORDER.MATERIALS` cycles a list of panes around the frame |
| Snowflake burst | `EFFECTS.PURCHASE` spawns particles around the player on every unwrap |

## Mystery present

```yaml
PRESENT:
  ENABLED: true
  MATERIAL: "RED_SHULKER_BOX"
  NAME: "&c❄ &aᴍʏꜱᴛᴇʀʏ ᴘʀᴇꜱᴇɴᴛ &c❄"
  SLOT: -1              # -1 = centered on the first row with no rewards in it
  CRATES: ["ALL"]       # or ["Gold", "Rare"]
  MAX-AMOUNT: 64
  SHOW-CHANCES: true
  CHANCE-LINE: "&8 ▪ &f%item% &8» &a%chance%%"
```

Odds go in `crates.yml` beside the rewards, keyed the same way. They are **weights**, not
percentages, and a reward with no entry weighs 1:

```yaml
Gold:
  CHANCES:
    '10': 50
    '11': 25
    '12': 25
```

With no `CHANCES` at all every reward is equally likely (7 rewards → 14.3% each).

Opening several presents rolls each one independently, so 5 presents can give 5 different gifts.
Each roll respects the reward's own stack size (Gold's 16 spawners stay 16) and its commands
(a rolled Ruby tool runs its `voyager` command instead of giving the icon). With
`DROP-OVERFLOW: false` opening stops when a gift no longer fits, and only the presents actually
opened are charged.

Chat summary: `You opened 5 present(s) from Gold and got: 48x spawner, 16x spawner`

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/crate [crate]` | `havoccrates.crate` | Open a crate menu from anywhere, or list the crates |
| `/cratealerts [on\|off]` | `havoccrates.alerts` | Silence or restore the crate messages |
| `/crates create\|delete\|set\|unset\|edit\|open\|list\|reload` | `havoccrates.admin` | Crate administration |
| `/crates debug [player]` | `havoccrates.admin` | Version, storage and alert state |
| `/key give\|giveall\|remove\|set\|check` | `havoccrates.admin` | Key management |

## Sounds and effects

```yaml
SOUNDS:
  BUTTON-CLICK: "minecraft:block.note_block.bell|0.7|1.6"
  PURCHASE: "minecraft:block.note_block.chime|1.0|1.4"
  ERROR: "minecraft:block.note_block.didgeridoo|0.8|0.6"

EFFECTS:
  PURCHASE: "SNOWFLAKE|35|0.6"     # PARTICLE|count|spread, "none" to disable
```

## Building

`mvn clean package` with **JDK 21** (paper-api 1.20.6 is compiled for Java 21). The shipped jar is
Java 17 bytecode, so it runs on Java 17 and 21 servers alike.

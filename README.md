# HavocCrates

Donut-style crates for Paper/Spigot **1.20+**. A crate block opens a reward menu; every reward
costs virtual keys. The confirm menu lets you buy in bulk with a **1 / 10 / 64** selector, and
unstackable rewards such as armour are handed over as individual pieces, so a bulk buy fills the
inventory instead of stopping at one item.

## Features

| Feature | Detail |
| --- | --- |
| Bulk buying | Green `+1 / +10 / +64` (3 slots) and red `-1 / -10 / -64` (3 slots) in the confirm menu |
| Armour stacking | Unstackable rewards are split into separate items and pushed into the inventory. With `DROP-OVERFLOW: true` the excess drops at your feet; with `false` nothing drops - the inventory is filled with as many as fit and you are only charged for those |
| Right click = stack | Right clicking a reward in the crate menu opens the confirm menu pre-loaded with a full stack, capped by the keys you own |
| Live preview | The reward in the middle of the confirm menu renders with the amount you selected |
| Amount cap | Never lets you select more than `MAX-AMOUNT` or more than your key balance |
| Shift click | Shift click a green button to jump to the max, a red button to reset to 1 |
| Storage | Flatfile by default, SQLite or MySQL optionally (falls back to flatfile if the driver is missing) |
| PlaceholderAPI | `%havoccrates_keys_<crate>%`, `%havoccrates_keys_total%` |
| Old crates.yml | Reads the previous plugin's file as-is: crates at the top level with TITLE / ROWS / LOCATIONS / COMMANDS / POSITIONS / REWARDS |
| Same alignment | Each crate uses its own ROWS and centers its rewards exactly like before (7 -> slots 10-16, 6 -> 10-15, 5 -> 11-15, 2 -> 12-13); a POSITION >= 0 pins a reward to that slot |
| Per item limits | `RESTRICTIONS` caps quantities per material and can hide the quantity buttons entirely (totems, shulkers, potions, bows...) |

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/crates create <crate>` | `havoccrates.admin` | Create a crate |
| `/crates delete <crate>` | `havoccrates.admin` | Delete a crate |
| `/crates set <crate>` | `havoccrates.admin` | Bind the block you are looking at to a crate |
| `/crates unset` | `havoccrates.admin` | Unbind the block you are looking at |
| `/crates edit <crate>` | `havoccrates.admin` | Open the reward editor (drag items in, close to save) |
| `/crates open <crate> [player]` | `havoccrates.admin` | Open a crate menu |
| `/crates list` | `havoccrates.admin` | List crates |
| `/crates reload` | `havoccrates.admin` | Reload config.yml and crates.yml |
| `/key check [player]` | – | Show key balances |
| `/key give <player> <crate> <amount>` | `havoccrates.admin` | Give keys |
| `/key giveall <crate> <amount>` | `havoccrates.admin` | Give keys to everyone online |
| `/key remove <player> <crate> <amount>` | `havoccrates.admin` | Remove keys |
| `/key set <player> <crate> <amount>` | `havoccrates.admin` | Set a key balance |

## Quick start

```
/crates create summer
/crates edit summer        # drag the rewards in, then close the GUI
/crates set summer         # look at a chest/shulker first
/key give <you> summer 640
```

Right click the block, right click a reward, tune the amount with the green/red buttons, confirm.

## Confirm menu layout (defaults)

```
 slot  9 10 11        13        15 16 17
       -64 -10 -1    reward    +1 +10 +64      (row 2)
 slot 18 .......... 22 .......... 26
      CANCEL       your keys      CONFIRM      (row 3)
```

Every material, name, lore, slot and value is configurable in `config.yml` under `CONFIRM-MENU`.
`AMOUNTS.ADD.VALUES` / `AMOUNTS.REMOVE.VALUES` line up index-by-index with their `SLOTS` lists,
so you can change `1, 10, 64` to anything (e.g. `1, 16, 32`) or add a fourth button.

## Reward commands

A reward slot can also fire console commands (`crates.yml` → `ITEMS.<slot>.COMMANDS`):

* `%player%`, `%crate%` and `%amount%` are replaced.
* If the command contains `%amount%` it runs **once** with the total; otherwise it runs **once per
  item bought**.

## Building

```
mvn clean package
```

Needs **JDK 21** - `paper-api` 1.20.6 is itself compiled for Java 21, so the plugin has to target
21 as well (that is also the Java version Minecraft 1.20.6+ servers run on).

Output: `target/HavocCrates-3.2.jar`. The included GitHub Actions workflow
(`.github/workflows/build.yml`) builds on every push, uploads the jar as an artifact, and attaches
it to a release when you push a `v*` tag.

### Building for a Java 17 server

If your server still runs Java 17, change two lines in `pom.xml` and the plugin builds for 17:

```xml
<maven.compiler.release>17</maven.compiler.release>
...
<version>1.20.1-R0.1-SNAPSHOT</version>   <!-- paper-api, the last Java 17 build -->
```

and set `java-version: '17'` in `.github/workflows/build.yml`.

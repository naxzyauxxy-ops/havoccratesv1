# HavocCrates

Donut-style crates for Paper/Spigot **1.20+**. A crate block opens a reward menu; every reward costs
virtual keys. The confirm menu buys in bulk with `Add 1 / Add 10 / Set to 64`, and unstackable
rewards such as armour are handed over as individual pieces so a bulk buy fills the inventory
instead of stopping at one item.

Reads the previous plugin's `crates.yml` as-is - no conversion needed.

## Features

| Feature | Detail |
| --- | --- |
| Bulk buying | Green `Add 1 / Add 10 / Set to 64` and red `Remove 1 / 10 / 64`, fully configurable |
| Self-tidying buttons | A quantity button is only drawn when it would change something, so "Remove 64" appears only once you have gone up to 64 |
| Armour stacking | Unstackable rewards are split into separate items and pushed into the inventory |
| Fills, never refuses | With `DROP-OVERFLOW: false` the inventory is filled with as many as fit and you are charged only for those; existing items are never replaced |
| Right click = stack | Right clicking a reward opens the confirm menu pre-loaded with a full stack, capped by your keys and by `RESTRICTIONS` |
| Per item limits | `RESTRICTIONS` caps quantities per material and can hide the quantity buttons entirely (totems, shulkers, potions, bows...) |
| Old crates.yml | Crates at the top level with `TITLE` / `ROWS` / `LOCATIONS` / `COMMANDS` / `POSITIONS` / `REWARDS` |
| Same alignment | Each crate uses its own `ROWS` and centers its rewards like before (7 -> slots 10-16, 6 -> 10-15, 5 -> 11-15, 2 -> 12-13) |
| Storage | Flatfile by default, SQLite or MySQL optionally |
| Toggleable messages | `/cratealerts` silences the purchase spam per player; errors always come through |
| PlaceholderAPI | `%havoccrates_keys_<crate>%`, `%havoccrates_keys_total%`, `%havoccrates_alerts_status%`, `%havoccrates_alerts%` |

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/crate` | `havoccrates.crate` (default: everyone) | List the crates and your keys for each |
| `/crate <crate>` | `havoccrates.crate` | Open that crate's menu from anywhere in the world |
| `/crate <crate> <player>` | `havoccrates.admin` | Open it for someone else |
| `/crates create <crate>` | `havoccrates.admin` | Create a crate |
| `/crates delete <crate>` | `havoccrates.admin` | Delete a crate |
| `/crates set <crate>` | `havoccrates.admin` | Bind the block you are looking at |
| `/crates unset` | `havoccrates.admin` | Unbind the block you are looking at |
| `/crates edit <crate>` | `havoccrates.admin` | Reward editor (drag items in, close to save) |
| `/crates open <crate> [player]` | `havoccrates.admin` | Open a crate menu |
| `/crates list` | `havoccrates.admin` | List crates |
| `/crates reload` | `havoccrates.admin` | Reload config.yml and crates.yml |
| `/cratealerts [on\|off]` | `havoccrates.alerts` (default: everyone) | Silence or restore the crate messages |
| `/key check [player]` | - | Show key balances |
| `/key give <player> <crate> <amount>` | `havoccrates.admin` | Give keys |
| `/key giveall <crate> <amount>` | `havoccrates.admin` | Give keys to everyone online |
| `/key remove <player> <crate> <amount>` | `havoccrates.admin` | Remove keys |
| `/key set <player> <crate> <amount>` | `havoccrates.admin` | Set a key balance |

Aliases: `/opencrate`, `/crateopen` for `/crate`; `/hcrates`, `/havoccrates`, `/cratesadmin` for
`/crates`; `/keys`, `/hkey` for `/key`.

`/crate` is configurable under `CRATE-COMMAND`:

```yaml
CRATE-COMMAND:
  SOUND: "block.chest.open"
  PER-CRATE-PERMISSION: false   # true -> also needs havoccrates.crate.<crate>
  REQUIRE-KEYS: false           # true -> only opens if the player holds a key for it
```

## Confirm menu

Six rows, framed, with the buttons spread out instead of packed into one line:

```
 B   B   B   B    B    B   B   B   B
 B   .   .   .   keys  .   .   .   B
 B  -64 -10  -1  ITEM  +1  +10 s64  B
 B   .   .   .    .    .   .   .   B
 B  [ C A N C E L ]  [ C O N F I R M ]  B
 B   B   B   B    B    B   B   B   B
```

Confirm and cancel are three slots wide (`SLOTS: [41, 42, 43]` / `[37, 38, 39]`), the reward sits in
the middle at 22 and the key counter above it at 13. `SIZE`, `ITEM-SLOT`, every `SLOT` / `SLOTS`,
the `FILLER` background and the `BORDER` frame are configurable.

### Buttons hide when they can't do anything

With `HIDE-UNUSABLE-BUTTONS: true` (default) a quantity button is drawn only when it would change
the amount:

| Amount | Buttons shown |
| --- | --- |
| 1 | Add 1, Add 10, Set to 64 |
| 11 | Remove 10, Remove 1, Add 1, Add 10, Set to 64 |
| 64 | Remove 64, Remove 10, Remove 1 |

Removes appear only once you have gone up that far, adds disappear at the limit, and an ender pearl
capped at 16 never shows "Set to 64".

### Quantity buttons

```yaml
QUANTITY_ADJUST:
  ADD:
    MATERIAL: "LIME_STAINED_GLASS_PANE"
    ADD_1:   { SLOT: 23, NAME: "&#00FC00Add 1",     INCREMENT: 1 }
    ADD_10:  { SLOT: 24, NAME: "&#00FC00Add 10",    INCREMENT: 10 }
    SET_64:  { SLOT: 25, NAME: "&#00FC00Set to 64", INCREMENT: 64 }
  REMOVE:
    MATERIAL: "RED_STAINED_GLASS_PANE"
    REMOVE_1:  { SLOT: 21, NAME: "&cRemove 1",  DECREMENT: 1 }
    REMOVE_10: { SLOT: 20, NAME: "&cRemove 10", DECREMENT: 10 }
    REMOVE_64: { SLOT: 19, NAME: "&cRemove 64", DECREMENT: 64 }
```

A button's behaviour comes from its name - `ADD_*` adds, `REMOVE_*` subtracts, `SET_*` sets the
amount, `MAX_*` jumps to the highest allowed, `MIN_*` / `RESET_*` goes back to the minimum - or set
`MODE: ADD / SUBTRACT / SET / MAX / MIN` explicitly. Add as many buttons as you like; each needs a
`SLOT` and an `INCREMENT` / `DECREMENT`. Shift clicking an add button jumps to the max, shift
clicking a remove button resets to the minimum.

## Turning the messages off

Buying in bulk is chatty, so every player can silence it with `/cratealerts` (aliases
`/togglecratealerts`, `/cratemessages`). `/cratealerts on` and `/cratealerts off` set it directly.

```yaml
ALERTS:
  DEFAULT: true                 # for players who never touched the setting
  STATUS-ENABLED: "&aEnabled"
  STATUS-DISABLED: "&cDisabled"
  TOGGLEABLE:                   # only these can be silenced
    - REWARD_RECEIVED
    - INVENTORY_PARTIAL
    - RECEIVED_KEYS
```

Anything not in `TOGGLEABLE` - `NOT_ENOUGH_KEYS`, `INVENTORY_FULL`, permission errors - is always
delivered, so nobody can silence themselves into confusion. The setting is saved per player
(flatfile, SQLite or MySQL) and survives relogs.

For a settings menu or scoreboard: `%havoccrates_alerts_status%` renders as Enabled / Disabled and
`%havoccrates_alerts%` gives true / false.

## Filling the inventory

Buying 64 of an unstackable reward needs 64 free slots and a player only has 36, so the purchase is
never refused outright:

* `DROP-OVERFLOW: true` - all 64 are handed over, whatever does not fit drops at your feet.
* `DROP-OVERFLOW: false` - the inventory is filled with as many as actually fit (36 in that
  example), you are charged only for those, and the rest is not bought. Matching stacks are topped
  up first, then empty slots are used, so nothing already in the inventory is replaced.
  `INVENTORY_FULL` now only fires when there is genuinely no room at all.

## Reward stack sizes

A reward saved in `crates.yml` with a stack size is handed over whole: `Gold` stores 16 spawners
per slot, so one key gives **16 spawners**, and `Loot` stores 64 blocks, so one key gives a full
stack. The amount selector counts *purchases* - buying 3 of a 16 spawner reward costs 3 keys and
gives 48 spawners.

That interacts with `RESTRICTIONS`:

* `RESTRICTIONS-IN-ITEMS: true` (default) - `MAX_QUANTITY` counts **items**, so
  `SPAWNER: { MAX_QUANTITY: 16 }` on a 16 spawner reward is exactly one purchase.
* `MAX_PURCHASES` on a restriction overrides the maths and caps how many times a reward can be
  bought at once, e.g. `DIAMOND_BLOCK: { MAX_PURCHASES: 10 }` allows 10 x 64 blocks.
* `RESTRICTIONS-IN-ITEMS: false` - `MAX_QUANTITY` counts purchases instead.

Lore placeholders: `%amount%` (purchases), `%items%` (total items), `%each%` (items per purchase).
Lines containing `%items%` are skipped automatically when a purchase is a single item.

## Per item restrictions

```yaml
RESTRICTIONS:
  TOTEM_OF_UNDYING: { MAX_QUANTITY: 1, MIN_QUANTITY: 1, HIDE_QUANTITY_BUTTONS: true }
  ENDER_PEARL:      { MAX_QUANTITY: 16, MIN_QUANTITY: 1 }
  SHULKER_BOX:      { MAX_QUANTITY: 1, MIN_QUANTITY: 1, HIDE_QUANTITY_BUTTONS: true }
  DEFAULT:          { MAX_QUANTITY: 64, MIN_QUANTITY: 1 }
```

A key matches the material exactly or as a suffix, so `SHULKER_BOX` also covers
`WHITE_SHULKER_BOX`, and the most specific match wins (`SPLASH_POTION` beats `POTION`). When a
reward is capped at one, the quantity buttons are not drawn and right clicking it opens at 1.

## crates.yml

The file from the old plugin is read as-is - crates sit at the top level:

```yaml
Common:
  TITLE: '&8choose 1 item'
  ROWS: 3
  LOCATIONS:
  - spawn,99,-21,70          # world,x,y,z  (world;x;y;z also works)
  COMMANDS:
    '11': 'voyager amethyst pickaxe {player} 7d'
  POSITIONS:
    '11': -1                 # -1 = auto centered, or a slot to pin it to
  REWARDS:
    '11':
      ==: org.bukkit.inventory.ItemStack
      ...
```

* `COMMANDS` may be keyed by reward key **or** by the slot the reward is drawn on - the old plugin
  wrote both (Ruby stores rewards as 1-5 and commands as 11-15). Both are matched up on load.
* `{player}`, `{crate}`, `{amount}` and the `%player%` style are all replaced.
* A command containing `{amount}` runs **once** with the total; otherwise it runs **once per item
  bought**. Empty command strings are ignored.
* `/crates edit <crate>` rewrites the file in the same format, pinning each reward to the slot you
  left it on. Nothing else ever writes to it - a hand edited file stays exactly as it is.

## Quick start

```
/crates create summer
/crates edit summer        # drag the rewards in, then close the GUI
/crates set summer         # look at a chest/shulker first
/key give <you> summer 640
```

## Building

```
mvn clean package
```

Needs **JDK 21** - `paper-api` 1.20.6 is itself compiled for Java 21, so the plugin targets 21 as
well (that is also the Java version Minecraft 1.20.6+ servers run on).

Output: `target/HavocCrates-3.6.jar`. The GitHub Actions workflow (`.github/workflows/build.yml`)
builds on every push, uploads the jar as an artifact, and attaches it to a release on a `v*` tag.

### Building for a Java 17 server

Change two lines in `pom.xml`:

```xml
<maven.compiler.release>17</maven.compiler.release>
...
<version>1.20.1-R0.1-SNAPSHOT</version>   <!-- paper-api, the last Java 17 build -->
```

and set `java-version: '17'` in `.github/workflows/build.yml`.

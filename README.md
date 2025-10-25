# FertileCrops

**FertileCrops** is a Minecraft plugin that enhances farming by allowing crops to spread using bone meal, with configurable success rates, XP costs, and radius. It also provides commands to manage allowed crops, withered blocks, and plugin settings.

---

https://youtu.be/orx1yQuhm6k

## Features

- Spread crops nearby using bone meal.
- Configurable success rate.
- XP cost, and spread radius.
- Allowed crops and withered (failure) blocks management.
- Commands configuration and inspection.
- Dynamic messages loaded from `messages.yml`.

## Installation

1. Download the plugin `.jar`.
2. Place it in your server's `plugins/` folder.
3. Start the server.
4. Edit `config.yml` and `messages.yml` to customize.

## Commands

All commands use the `/fc` or `/fertilecrops` prefix.

| Command | Description |
|---------|-------------|
| `/fc crop add <MATERIAL>` | Adds a crop to the allowed list. |
| `/fc crop remove <MATERIAL>` | Removes a crop from the allowed list. |
| `/fc crop list` | Lists all allowed crops. |
| `/fc withered add <MATERIAL>` | Adds a block to the withered list. |
| `/fc withered remove <MATERIAL>` | Removes a block from the withered list. |
| `/fc withered list` | Lists all configured withered blocks. |
| `/fc cost <xp>` | Sets the XP cost for crop spread. |
| `/fc rate <0.0-1.0>` | Sets the success chance of crop spread. |
| `/fc radius <blocks>` | Sets the spread radius for crop spread. |
| `/fc reload` | Reloads plugin config and messages. |
| `/fc help` | Shows all commands with their description. |

You can find the MATERIAL_ID here https://minecraft-ids.grahamedgecombe.com/

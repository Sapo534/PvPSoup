# 🥣 PvPSoup

**PvPSoup** is a client-side mod for Minecraft on Fabric (version 1.21.x), designed to automate PvP mechanics involving soups (Soup PvP).

---

## ✨ Features

- **Auto-Refill** — instantly refills your hotbar with soups from your inventory.
- **Auto-Drop Bowls** — automatically drops empty bowls from your hotbar and inventory.
- **Auto-Eat** — quickly consumes soup via a keybind or health threshold (Health Trigger).
- **Flexible configuration** — full control over features via in-game commands.

---

## 🎮 Commands & Controls

The mod's main command is `/pvpsoup`.

| Command | Description | Default Value |
| :--- | :--- | :--- |
| `/pvpsoup toggle <true\|false>` | Global mod toggle | `true` |
| `/pvpsoup refill <true\|false>` | Auto-refill hotbar | `true` |
| `/pvpsoup dropbowls <true\|false>` | Auto-drop empty bowls | `true` |
| `/pvpsoup autoeat <true\|false>` | Auto-eat soups | `true` |
| `/pvpsoup trigger <key\|health>` | Eat trigger mode (`key` — by keybind, `health` — by HP) | `key` |
| `/pvpsoup health <hearts>` | HP threshold for `health` mode (in hearts, e.g. `7.0`) | `7.0` |
| `/pvpsoup help` | Display command help | — |

> ⌨️ **Default keybind:** **`R`** (can be rebound in Minecraft's standard controls settings).

---

## 🛠️ Building from Source

### Requirements
- **JDK 21** or higher

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/pvpsoup.git
   cd pvpsoup
   ```

2. Build the mod using Gradle:
   ```bash
   ./gradlew build
   ```

3. The compiled `.jar` file will be located in the `build/libs/` directory.

---

## 📜 License

This project is distributed under the **GNU General Public License v3.0 (GPL-3.0)**. For more details, see the [LICENSE](https://www.google.com/search?q=LICENSE) file.

# Silent Gear Catalog - Feature Overview

## Overview

**Silent Gear Catalog** is a convenient query tool for the Silent Gear mod, allowing players to quickly look up detailed information on all materials and traits without having to browse through crafting recipes or switch between interfaces.

---

## Core Features

### 🔍 Smart Search
Supports real-time search by name, ID, trait name, and attribute name. Simply enter a keyword to instantly locate the desired material or trait.

### 📋 Material Browsing
- Displays all materials in a clear list
- Each material shows its name, grade, and number of traits
- Sort by name, grade, or trait count
- Toggle between ascending and descending order

### ⚡ Trait Browsing
- Lists all traits with their detailed descriptions
- Click on a trait to view all materials that possess it

### 🎯 Attribute Filtering
Filter materials by whether they possess specific attributes.

### 🔧 Part-Type Filtering
Filter materials by the part types they support:
- Main Part, Tool Handle, Tip Upgrade, Cord, Fletching, Binding, Coating, Grip, Lining, Embedding, Upgrade Part

### 📊 Attribute Sorting
Sort materials by any attribute value:
- Name, Grade, Trait Count
- Durability, Armor, Attack Damage, Mining Speed, and all other numeric attributes
- Toggle between ascending and descending order

### 📖 Material Detail Panel
Click on a material row to view detailed information:
- Displays grouped by part type (Main Part, Handle, Tip, etc.)
- Each part shows its own attributes and traits individually
- Part-type filtering sync: after selecting a part type, the detail panel only shows information for that part
- Trait names are clickable to jump to the list of materials that have that trait

### 🔗 Trait Cross-Linking
- Trait names in material details are directly clickable
- Automatically jumps to the list of materials containing that trait
- The trait list provides the same functionality

### 🎨 Intuitive Interface
- Left sidebar for quick switching between materials and traits
- Adaptive window sizing

---

## How to Use

1. **Item Trigger**: Right-click while holding the book item
2. **Interface Operations**: Search via the search box, filter using filter buttons, sort with sort buttons, and click entries to view details

---

## Use Cases

| Scenario | Description |
|----------|-------------|
| Finding specific materials | Quickly locate with attribute filtering + sorting |
| Looking up trait effects | View descriptions directly on the trait page |
| Understanding material uses | Check which part types a material supports |
| Trait source lookup | Click on a trait to see which materials have it |
| Material comparison | Sort functionality for quick attribute comparisons |

---

## Technical Features

- Reads data in real-time through the Silent Gear API
- Lightweight UI with no impact on game performance

---

**Making Silent Gear material queries easier than ever before!**
package com.example.game.model

enum class Age(val displayName: String, val index: Int) {
  DARK_AGE("Dark Age", 0),
  FEUDAL_AGE("Feudal Age", 1),
  CASTLE_AGE("Castle Age", 2),
  IMPERIAL_AGE("Imperial Age", 3),
  INDUSTRIAL_AGE("Industrial Age", 4),
  MODERN_AGE("Modern Age", 5),
}

enum class Civilization(
  val displayName: String,
  val specialty: String,
  val bonusText: String,
  val accentHex: Long,
) {
  BRITONS(
    displayName = "Britons",
    specialty = "Foot Archers",
    bonusText = "Archers +1 Range • Archery Ranges train 15% faster",
    accentHex = 0xFFD4A338,
  ),
  FRANKS(
    displayName = "Franks",
    specialty = "Heavy Cavalry",
    bonusText = "Cavalry +20% HP • Farms cost 25% less Wood",
    accentHex = 0xFF3E7B42,
  ),
  TEUTONS(
    displayName = "Teutons",
    specialty = "Armored Infantry",
    bonusText = "Infantry +1 Melee Armor • Towers & Castles +25% HP",
    accentHex = 0xFF8C5328,
  ),
  BYZANTINES(
    displayName = "Byzantines",
    specialty = "Defense & Counters",
    bonusText = "Buildings +20% HP • Spearmen & Skirmishers 20% cheaper",
    accentHex = 0xFF7D3B8C,
  ),
}

enum class MapType(val displayName: String, val description: String) {
  ARABIA("Arabia", "Open rolling plains with contested central gold hills"),
  BLACK_FOREST("Black Forest", "Dense ancient woodlands with strategic choke passages"),
  COASTAL("Coastal Crossing", "Winding river valley bridged by stone fords"),
}

enum class MapSizeOption(val displayName: String, val tiles: Int, val subtitle: String) {
  STANDARD("Standard (60×60)", 60, "Crisp 2–3 empire skirmish"),
  LARGE("Large (76×76)", 76, "Vast realms for up to 4 empires"),
  HUGE("Huge (92×92)", 92, "Continental grand campaign"),
}

enum class Difficulty(
  val displayName: String,
  val aiGatherMultiplier: Float,
  val aiTrainRate: Float,
  val summary: String,
) {
  STANDARD("Standard", 0.425f, 0.425f, "Relaxed • 43% Pace"),
  MODERATE("Moderate", 0.50f, 0.525f, "Balanced • 50% Pace"),
  HARD("Hard", 0.60f, 0.625f, "Tactical • 60% Pace"),
  EXTREME("Extreme", 1.20f, 1.25f, "Classic Hard • 120% Eco"),
}

enum class BuildCategory(val displayName: String) {
  ECONOMY("Economy"),
  MILITARY("Military"),
}

enum class Faction(val displayName: String, val bannerHex: Long, val isAi: Boolean) {
  PLAYER("Royal Crown", 0xFF2E5EA3, false),
  ENEMY("Crimson Warlord", 0xFFB0342F, true),
  ENEMY_2("Emerald Khanate", 0xFF287D48, true),
  ENEMY_3("Imperial Violet", 0xFF7B3B96, true);

  companion object {
    val aiFactions: List<Faction> = listOf(ENEMY, ENEMY_2, ENEMY_3)
  }
}

enum class ResourceType(val displayName: String) {
  WOOD("Wood"),
  FOOD("Food"),
  GOLD("Gold"),
  STONE("Stone"),
}

enum class TileTerrain {
  GRASS,
  GRASS_ALT,
  DIRT_ROAD,
  WATER,
  CLIFF,
}

enum class ResourceNodeType(val displayName: String, val resourceType: ResourceType) {
  FOREST_OAK("Oak Tree", ResourceType.WOOD),
  FOREST_PINE("Pine Tree", ResourceType.WOOD),
  BERRY_BUSH("Forage Bush", ResourceType.FOOD),
  DEER("Wild Deer", ResourceType.FOOD),
  GOLD_MINE("Gold Mine", ResourceType.GOLD),
  STONE_QUARRY("Stone Quarry", ResourceType.STONE),
}

enum class BuildingType(val displayName: String) {
  TOWN_CENTER("Town Center"),
  HOUSE("House"),
  LUMBER_CAMP("Lumber Camp"),
  MINING_CAMP("Mining Camp"),
  MILL("Mill"),
  FARM("Farm"),
  BARRACKS("Barracks"),
  ARCHERY_RANGE("Archery Range"),
  STABLE("Stable"),
  BLACKSMITH("Blacksmith"),
  UNIVERSITY("University"),
  WATCH_TOWER("Watch Tower"),
  CASTLE("Castle"),
  WAR_FACTORY("War Factory"),
  AIRFIELD("Airfield"),
}

enum class UnitType(val displayName: String) {
  VILLAGER("Villager"),
  MILITIA("Militia"),
  MAN_AT_ARMS("Man-at-Arms"),
  SPEARMAN("Spearman"),
  PIKEMAN("Pikeman"),
  ARCHER("Archer"),
  CROSSBOWMAN("Crossbowman"),
  SKIRMISHER("Skirmisher"),
  SCOUT_CAVALRY("Scout Cavalry"),
  KNIGHT("Knight"),
  BATTERING_RAM("Battering Ram"),
  RIFLEMAN("Rifleman"),
  MACHINE_GUNNER("MG Trooper"),
  MARINE("Assault Marine"),
  SNIPER("Sniper"),
  ROCKET_SOLDIER("Rocket Trooper"),
  ARTILLERY("Field Artillery"),
  TANK("Battle Tank"),
  HEAVY_TANK("Heavy Tank"),
  ROCKET_LAUNCHER("MLRS Battery"),
  FIGHTER_PLANE("Fighter Jet"),
  BOMBER_PLANE("Heavy Bomber"),
  GUNSHIP("Attack Gunship"),
}

enum class TechType(val displayName: String, val shortDesc: String) {
  ADVANCE_FEUDAL("Feudal Age", "Unlocks Archery Range, Stable, Blacksmith & Watch Tower"),
  ADVANCE_CASTLE("Castle Age", "Unlocks Knights, Crossbowmen, Battering Ram & Castle"),
  ADVANCE_IMPERIAL("Imperial Age", "All units +20% HP & +2 Attack power spike"),
  ADVANCE_INDUSTRIAL("Industrial Age", "Unlocks War Factory, Riflemen, Field Artillery & Battle Tanks (+2 Attack)"),
  ADVANCE_MODERN("Modern Age", "Unlocks Airfield, Assault Marines, Heavy Tanks, Fighter Jets & Heavy Bombers (+3 Attack)"),

  // Mill Agrarian & Logistics Researches
  STONE_GRINDING("Millstones", "+15% Food gather speed & +10% Villager move speed"),
  HORSE_COLLAR("Horse Collar", "+25% Villager harvest rate on Farms & resource nodes"),
  WHEELBARROW("Wheelbarrow", "+25% Villager move speed & +20 Villager HP"),
  HEAVY_PLOW("Heavy Plow", "+5 Villager carry capacity & +20% Farm harvest rate"),
  GRANARY_SILOS("Granary Silos", "+4 Villager carry capacity & +20% Farm harvest speed"),
  CROP_ROTATION("Crop Rotation", "+35% Food & Wood gathering speed"),
  MECHANIZED_FARMING("Mechanized Farming", "+30% Villager work & move speed, +5 carry capacity"),
  SUPPLY_LOGISTICS("Supply Logistics", "+40% all gather rates & 15% faster unit training"),

  // Mining Camp Extraction Researches
  GOLD_MINING("Gold Mining", "+25% Gold mining speed"),
  STONE_MINING("Stone Mining", "+25% Stone quarrying speed"),
  GOLD_SHAFT_MINING("Gold Shaft Mining", "+35% Gold mining speed & +3 carry capacity"),
  STONE_SHAFT_MINING("Stone Shaft Mining", "+35% Stone quarrying speed & +10% Building HP"),
  INDUSTRIAL_EXCAVATION("Steam Excavation", "+40% Gold & Stone gather speed & +4 carry capacity"),

  // University Scholarly, Fortification & Science Researches
  MASONRY("Masonry", "+15% Building HP & +35% Villager repair speed"),
  TREADMILL_CRANE("Treadmill Crane", "+35% faster building construction speed"),
  BALLISTICS("Ballistics", "+1 Ranged Attack, +2 Defense Attack & +30% faster projectiles"),
  ARCHITECTURE("Architecture", "+20% Building HP across all structures"),
  SIEGE_ENGINEERS("Siege Engineers", "+1 Siege Range & +35 Siege damage vs Buildings"),
  CHEMISTRY("Chemistry", "+3 Attack for Gunpowder Infantry, Artillery, Tanks, Rockets & Aircraft"),
  ACADEMIC_CONSCRIPTION("Military Academy", "20% faster unit training at all military buildings"),
  AERODYNAMICS("Aerodynamics", "+15% Aircraft speed & +4 Aircraft Attack"),

  // Blacksmith Arms & Armor Researches
  FORGING("Forging", "+2 Melee & Armored Attack"),
  FLETCHING("Fletching", "+1 Attack & +1 Range for Ranged Units, Towers & Castles"),
  SCALE_ARMOR("Scale Armor", "+2 Melee & Pierce Armor for Infantry & Cavalry"),
  IRON_CASTING("Iron Casting", "+2 Melee & Armored Attack"),
  CHAIN_MAIL("Chain Mail", "+2 Melee & Pierce Armor for Infantry & Cavalry"),
  BODKIN_ARROW("Bodkin & Rifling", "+2 Ranged Attack & +1 Range for Ranged Units & Defenses"),
  BLAST_FURNACE("Blast Furnace", "+3 Melee & Tank Attack"),
  PLATE_ARMOR("Plate Armor", "+3 Melee & Pierce Armor for all Military Units"),
  BALLISTICS_ALLOYS("Ballistic Alloys", "+3 Attack & +1 Range for Ranged, Tanks & Aircraft"),
  REACTIVE_ARMOR("Reactive Armor", "+3 All Armor & +30 HP for all Military Units"),

  // Unit Class Upgrades
  UPGRADE_MAN_AT_ARMS("Man-at-Arms", "Upgrades Militia to armored Man-at-Arms"),
  UPGRADE_PIKEMAN("Pikeman", "Upgrades Spearmen to deadly Pikemen"),
  UPGRADE_CROSSBOW("Crossbowman", "Upgrades Archers to high-damage Crossbowmen"),
}

enum class UnitOrderType {
  IDLE,
  MOVE,
  GATHER,
  BUILD,
  ATTACK_UNIT,
  ATTACK_BUILDING,
  ATTACK_MOVE,
}

enum class UnitStance {
  AGGRESSIVE,
  DEFENSIVE,
  STAND_GROUND,
}

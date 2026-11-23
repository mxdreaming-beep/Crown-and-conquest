package com.example.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.game.model.Age
import com.example.game.model.BuildCategory
import com.example.game.model.BuildingType
import com.example.game.model.Civilization
import com.example.game.model.Difficulty
import com.example.game.model.Faction
import com.example.game.model.MapSizeOption
import com.example.game.model.MapType
import com.example.game.model.ResourceType
import com.example.game.model.TechType
import com.example.game.model.UnitType

enum class AppLanguage(val code: String, val nativeName: String) {
  EN("en", "English"),
  FR("fr", "Français"),
  ES("es", "Español"),
  DE("de", "Deutsch"),
  TL("tl", "Tagalog"),
  ZH("zh", "中文"),
  KO("ko", "한국어"),
  JA("ja", "日本語"),
  RU("ru", "Русский"),
  UK("uk", "Українська"),
  EL("el", "Ελληνικά"),
  ;

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
  }
}

data class AppStrings(
  val appTitle: String,
  val gameSubtitle: String,
  val splashDescription: String,
  val languageLabel: String,
  val playSkirmish: String,
  val overmapAndFaq: String,
  val muteMusic: String,
  val unmuteMusic: String,
  val muteSfx: String,
  val unmuteSfx: String,
  // Skirmish Setup
  val warRoomTitle: String,
  val marshalRealm: String,
  val marshalRealmSub: String,
  val yourCivLabel: String,
  val aiRivalsLabel: String,
  val rivalDuel: String,
  val rivalThree: String,
  val rivalFour: String,
  val aiOneLabel: String,
  val aiTwoLabel: String,
  val aiThreeLabel: String,
  val battlefieldSizeLabel: String,
  val tilesUnit: String,
  val mapTypeLabel: String,
  val difficultyLabel: String,
  val launchBattle: String,
  // HUD & Controls
  val idleVillager: String,
  val workersLabel: String,
  val boxSelect: String,
  val dragBox: String,
  val tcShort: String,
  val armyLabel: String,
  val cancel: String,
  val back: String,
  val autoGather: String,
  val economyTitle: String,
  val economySub: String,
  val militaryTitle: String,
  val militarySub: String,
  val needPrefix: String,
  val attackMove: String,
  val tapTarget: String,
  val engageFoes: String,
  val holdLabel: String,
  val stopUnits: String,
  val allArmy: String,
  val rallyTroops: String,
  val townCenterAction: String,
  val trainAndAges: String,
  val idleLabel: String,
  val assignJob: String,
  val focusWorker: String,
  val selectArmy: String,
  val readySuffix: String,
  val villagerSingular: String,
  val villagersPlural: String,
  val taskIdle: String,
  val taskGathering: String,
  val taskConstructing: String,
  val taskMoving: String,
  val taskActive: String,
  val tapNodeOrBuild: String,
  val queueLabel: String,
  val trainingPrefix: String,
  val researchingPrefix: String,
  val buildingProgressPrefix: String,
  val tapSiteToBuild: String,
  val permanentLabel: String,
  val moveLabel: String,
  val movingLabel: String,
  val reqPrefix: String,
  val requiresAgePrefix: String,
  val requiresPrefix: String,
  val needResourcesPrefix: String,
  val queueFullMsg: String,
  val popLimitMsg: String,
  val tcRadiusErrorMsg: String,
  val invalidTerrainErrorMsg: String,
  val moveInvalidTerrainErrorMsg: String,
  val tapMapToPlacePrefix: String,
  val tapLocationToMovePrefix: String,
  val relocatedPrefix: String,
  val queuedPrefix: String,
  val buildRadiusHint: String,
  val tcExpandHint: String,
  val noVillagersMsg: String,
  val noResourcesNearbyMsg: String,
  val trainTroopsHintMsg: String,
  val selectedUnitsPrefix: String,
  val gameSpeedPrefix: String,
  val skirmishStartHint: String,
  // Pause & Post-Game
  val battlePaused: String,
  val resumeMatch: String,
  val sfxOn: String,
  val sfxOff: String,
  val musicOn: String,
  val musicOff: String,
  val restartSkirmish: String,
  val returnToMenu: String,
  val victory: String,
  val defeat: String,
  val durationPrefix: String,
  val rivalChronicle: String,
  val defeatedLabel: String,
  val bldgsLeftSuffix: String,
  val killsLabel: String,
  val razedLabel: String,
  val categoryLabel: String,
  val ageReached: String,
  val woodGathered: String,
  val foodGathered: String,
  val goldGathered: String,
  val stoneGathered: String,
  val unitsTrained: String,
  val enemiesDefeated: String,
  val unitsLost: String,
  val buildingsBuilt: String,
  val buildingsRazed: String,
  val mainMenu: String,
  val playAgain: String,
  // Codex / Guide
  val codexTitle: String,
  val codexSubtitle: String,
  val tabOvermap: String,
  val tabCosts: String,
  val tabCivs: String,
  val tabFaq: String,
  val overmapTitle: String,
  val overmapSub: String,
  val battlegroundsTitle: String,
  val battlegroundsSub: String,
  val startingEra: String,
  val buildingsHeader: String,
  val unitsHeader: String,
  val upgradesHeader: String,
  val ecoBuildingsTitle: String,
  val ecoBuildingsSub: String,
  val milBuildingsTitle: String,
  val milBuildingsSub: String,
  val unitRosterTitle: String,
  val unitRosterSub: String,
  val techCentersTitle: String,
  val techCentersSub: String,
  val civsTitle: String,
  val civsSub: String,
  val activeLabel: String,
  val faqTitle: String,
  val faqSub: String,
  val faqEntries: List<Pair<String, String>>,
  // Domain Dictionaries
  val ages: Map<Age, String>,
  val ageSummaries: Map<Age, String>,
  val civNames: Map<Civilization, String>,
  val civSpecialties: Map<Civilization, String>,
  val civBonuses: Map<Civilization, String>,
  val civStrategies: Map<Civilization, String>,
  val mapNames: Map<MapType, String>,
  val mapDescriptions: Map<MapType, String>,
  val mapSizeNames: Map<MapSizeOption, String>,
  val diffNames: Map<Difficulty, String>,
  val diffSummaries: Map<Difficulty, String>,
  val categories: Map<BuildCategory, String>,
  val factions: Map<Faction, String>,
  val resources: Map<ResourceType, String>,
  val buildingNames: Map<BuildingType, String>,
  val buildingDescriptions: Map<BuildingType, String>,
  val unitNames: Map<UnitType, String>,
  val techNames: Map<TechType, String>,
  val techDescriptions: Map<TechType, String>,
) {
  fun ageName(age: Age): String = ages[age] ?: age.displayName
  fun ageShort(age: Age): String = ageName(age).replace(" Age", "").replace("Âge ", "").replace("Edad ", "").trim()
  fun ageSummary(age: Age): String = ageSummaries[age] ?: ""
  fun civName(civ: Civilization): String = civNames[civ] ?: civ.displayName
  fun civSpecialty(civ: Civilization): String = civSpecialties[civ] ?: civ.specialty
  fun civBonus(civ: Civilization): String = civBonuses[civ] ?: civ.bonusText
  fun civStrategy(civ: Civilization): String = civStrategies[civ] ?: ""
  fun mapName(map: MapType): String = mapNames[map] ?: map.displayName
  fun mapDesc(map: MapType): String = mapDescriptions[map] ?: map.description
  fun mapSizeName(opt: MapSizeOption): String = mapSizeNames[opt] ?: opt.displayName.substringBefore(" (")
  fun diffName(diff: Difficulty): String = diffNames[diff] ?: diff.displayName
  fun diffSummary(diff: Difficulty): String = diffSummaries[diff] ?: diff.summary
  fun categoryName(cat: BuildCategory): String = categories[cat] ?: cat.displayName
  fun factionName(faction: Faction): String = factions[faction] ?: faction.displayName
  fun resourceName(res: ResourceType): String = resources[res] ?: res.displayName
  fun buildingName(type: BuildingType): String = buildingNames[type] ?: type.displayName
  fun buildingDesc(type: BuildingType): String = buildingDescriptions[type] ?: ""
  fun unitName(type: UnitType): String = unitNames[type] ?: type.displayName
  fun techName(type: TechType): String = techNames[type] ?: type.displayName
  fun techDesc(type: TechType): String = techDescriptions[type] ?: type.shortDesc
}

val LocalAppStrings = staticCompositionLocalOf { englishStrings() }

fun stringsFor(language: AppLanguage): AppStrings =
  when (language) {
    AppLanguage.EN -> englishStrings()
    AppLanguage.FR -> frenchStrings()
    AppLanguage.ES -> spanishStrings()
    AppLanguage.DE -> germanStrings()
    AppLanguage.TL -> tagalogStrings()
    AppLanguage.ZH -> chineseStrings()
    AppLanguage.KO -> koreanStrings()
    AppLanguage.JA -> japaneseStrings()
    AppLanguage.RU -> russianStrings()
    AppLanguage.UK -> ukrainianStrings()
    AppLanguage.EL -> greekStrings()
  }

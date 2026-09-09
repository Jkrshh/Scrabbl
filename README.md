# Scrabbl AI — solveur Scrabble français en temps réel 🎯📷

App Android qui trouve **les meilleurs coups au Scrabble en direct** en filmant
le plateau avec la caméra. 100 % français (ODS), détection automatique du type
de plateau (11×11 → 21×21), tout fonctionne hors-ligne une fois le
dictionnaire téléchargé.

---

## ✨ Fonctionnalités

| | |
|---|---|
| 📷 Caméra live | CameraX + ML Kit Text Recognition analysent chaque frame en continu |
| 🎯 Détection auto | Clusterisation des positions des lettres → grille N×N détectée (11, 13, 15 ou 21) |
| 🧠 Moteur intelligent | Algorithme Appel-Jacobson avec cross-checks, en Kotlin pur, <100 ms sur un plateau 15×15 |
| 🇫🇷 ODS français | Dictionnaire officiel du Scrabble francophone (~400 k mots) téléchargé au 1er lancement |
| 🃏 Joker (`?`) | Le solveur tente toutes les lettres possibles pour chaque joker |
| 🏆 Classement des coups | 100 meilleurs coups triés par score, avec scrabble +50 pts si les 7 tuiles sont posées |
| ✍️ Éditeur manuel | Corrige la détection ou saisis une partie sans caméra |
| 🎨 AR overlay | Le meilleur coup est surligné directement sur la caméra |

---

## 🚀 Build

Prérequis :

- Android Studio Ladybug (2024.2) ou +
- SDK Android 34, JDK 17
- Un appareil ou émulateur Android 8.0+ (API 26+)

```bash
git clone https://github.com/jkrshh/scrabbl.git
cd scrabbl
./gradlew assembleRelease            # génère app/build/outputs/apk/release/app-release.apk
# ou bien
./gradlew installDebug               # installe direct sur l'appareil branché
```

> **Note wrapper Gradle** : le dépôt contient `gradle-wrapper.properties` mais
> pas le JAR (limitation d'API GitHub). Après le premier `clone`, lance
> `gradle wrapper` (Android Studio le fait automatiquement lors de
> l'ouverture du projet), ou télécharge manuellement le wrapper 8.7 sur
> https://services.gradle.org/distributions/gradle-8.7-bin.zip.

---

## 🔬 Comment ça marche

### Pipeline vision

```
CameraX ImageAnalysis  →  ML Kit OCR (symbol par symbol)
                        ↘
                          BoardDetector : clustering 1D des centres X/Y
                                       ↓
                          BoardType déduit (11 / 13 / 15 / 21)
                                       ↓
                          Board 2D matrix rempli
```

Le détecteur ne cherche pas les contours du plateau (fragile selon
l'éclairage) : il **utilise directement les positions des lettres reconnues**.
C'est beaucoup plus robuste, quel que soit le design du plateau (physique,
photo, écran d'ordi, plateau customisé…).

Une case n'est publiée comme « occupée » qu'après 2 observations
consécutives (`BoardAnalyzer.stabilize`) — cela élimine le flicker sur du
texte parasite (dictionnaire imprimé, annotations, etc.).

### Moteur Scrabble

L'algorithme suit fidèlement **Appel-Jacobson** *(The World's Fastest
Scrabble Program, 1988)* :

1. **Cross-checks** — pour chaque case vide, on précalcule quelles lettres
   forment un mot perpendiculaire valide.
2. **Extension récursive** — pour chaque position de départ, on étend le
   mot lettre par lettre en piochant dans le rack, en élaguant via une
   recherche binaire de préfixe (`Dictionary.hasPrefix`).
3. **Transposition** — pour la direction verticale, on transpose le
   plateau et on relance la génération horizontale, puis on retourne les
   coordonnées.
4. **Scoring exact ODS** — multiplicateurs de lettre/mot, mots
   perpendiculaires, bonus scrabble à +50 pts, jokers à 0 pt.

### Dictionnaire

Deux sources :

1. `assets/starter_fr.txt` — quelques milliers de mots courants, embarqués
   dans l'APK. L'app est utilisable **immédiatement** après installation.
2. Téléchargement à la volée de l'ODS complet (~400 k mots) au premier
   lancement, en cache dans `filesDir/dict/ods_fr.txt.gz`. Miroirs
   configurables dans `DictionaryDownloader.SOURCES`.

Une fois téléchargé, le dictionnaire est chargé instantanément à chaque
démarrage suivant.

---

## 📁 Structure

```
scrabbl/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── assets/starter_fr.txt          ← dictionnaire de démarrage
│   ├── kotlin/com/scrabbl/ai/
│   │   ├── MainActivity.kt
│   │   ├── ScrabblApp.kt
│   │   ├── MainViewModel.kt
│   │   ├── engine/                    ← moteur Scrabble
│   │   │   ├── FrenchScrabble.kt      ← valeurs de tuiles, distribution
│   │   │   ├── BoardType.kt           ← types + layouts de primes
│   │   │   ├── Board.kt
│   │   │   ├── Move.kt
│   │   │   ├── Dictionary.kt
│   │   │   ├── MoveGenerator.kt       ← Appel-Jacobson
│   │   │   └── Scoring.kt
│   │   ├── dict/                      ← chargement ODS
│   │   │   ├── DictionaryRepository.kt
│   │   │   └── DictionaryDownloader.kt
│   │   ├── vision/                    ← détection plateau
│   │   │   ├── LetterRecognizer.kt    ← ML Kit
│   │   │   ├── BoardDetector.kt       ← clustering positions
│   │   │   └── BoardAnalyzer.kt       ← CameraX ImageAnalysis.Analyzer
│   │   └── ui/                        ← Jetpack Compose
│   │       ├── MainScreen.kt
│   │       ├── CameraScreen.kt
│   │       ├── BoardEditorScreen.kt
│   │       ├── MovesScreen.kt
│   │       ├── RackInput.kt
│   │       └── theme/Theme.kt
│   └── res/                           ← strings, icônes, thème
├── build.gradle.kts                   ← Kotlin 1.9.24, AGP 8.5.2
├── app/build.gradle.kts               ← Compose 2024.09, ML Kit, CameraX, OkHttp
└── settings.gradle.kts
```

---

## 🧪 Tester le moteur en isolation

Le moteur ne dépend que de Kotlin standard — pas d'Android. On peut donc
l'utiliser dans un test JVM :

```kotlin
val dict = Dictionary.from(listOf("BONJOUR", "OUI", "OUR", "SUIS", "SUIT"))
val board = Board(BoardType.CLASSIC)
val rack = intArrayOf(
    FrenchScrabble.l('B'), FrenchScrabble.l('O'), FrenchScrabble.l('N'),
    FrenchScrabble.l('J'), FrenchScrabble.l('O'), FrenchScrabble.l('U'),
    FrenchScrabble.l('R'),
)
val moves = MoveGenerator(dict).generate(board, rack)
println(moves.take(5).joinToString("\n") { "${it.word} @ ${it.humanCoord(15)} = ${it.score}" })
```

---

## 📝 Roadmap / limites connues

- L'overlay AR est en coordonnées image, pas en coordonnées view. La
  boîte affichée peut être décalée si l'aspect-ratio caméra ≠ écran ;
  seuls les indicateurs de position (case highlight) sont approximatifs.
  À terme, brancher `PreviewView.getMeteringPointFactory()` pour un
  mapping exact.
- Le téléchargement ODS dépend de miroirs GitHub publics — un onglet
  Réglages permettant de coller une URL personnalisée serait utile.
- Le dictionnaire est chargé entier en RAM (~60 MB). Un DAWG compact
  serait plus économe (~5 MB) et à peine plus lent.
- Support des lettres accentuées : l'ODS n'utilise pas d'accents ; les
  entrées OCR accentuées sont normalisées (`É → E`).

---

## 📜 Licence

Ce projet est fourni tel quel, à des fins d'apprentissage et d'usage
personnel. L'algorithme suit l'article public de Appel-Jacobson
(*Communications of the ACM*, 1988). Le dictionnaire ODS reste la
propriété de la Fédération Internationale de Scrabble Francophone.

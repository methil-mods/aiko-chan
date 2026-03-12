# Aiko Android

Application Android pour interagir avec Aiko-chan et débloquer de nouveaux personnages via NFC.

## Fonctionnalités NFC

L'application intègre un scanner NFC capable de lire des tags et de débloquer des personnages exclusifs.

### Foreground Dispatch
L'application utilise le **Foreground Dispatch System**. Cela signifie que lorsqu'elle est ouverte au premier plan, elle a la priorité absolue sur les scans NFC. Android ne vous demandera pas quelle application utiliser ; Aiko interceptera directement le tag.

### Déblocage de Personnages
Chaque scan de tag NFC envoie l'ID au backend pour vérifier si un personnage y est associé.
- Si le tag est valide, le personnage est lié à votre compte.
- Les logs de scan sont visibles dans Logcat avec le tag `NFC_SCANNER`.

## Tags de Test

| Personnage | Tag ID (HEX) | Description |
|------------|--------------|-------------|
| **Aiko**   | *(Aucun)*    | Débloquée par défaut à l'inscription. |
| **Yaku**   | `04A5831E700000` | Hackeuse rebelle débloquable via NFC. |

## Configuration
Le fichier `bridge/src/main/java/com/methil/aiko/data/AikoConfig.kt` contient l'`BASE_URL` du backend. Assurez-vous qu'elle pointe vers l'IP correcte de votre serveur.

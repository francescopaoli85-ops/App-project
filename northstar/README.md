# Northstar

App Android (Kotlin + Jetpack Compose) che trasforma un desiderio vago in un **obiettivo ben formato** secondo i 6 criteri PNL.
Specifiche: doc *Northstar — Specifiche per sviluppo*. Tema: **Notte Neon**.
Mockup di riferimento (8 schermate, apribili nel browser come sorgente HTML): `design/mockup/`.

## Provarla subito

1. Apri la cartella `northstar/` con Android Studio.
2. Premi ▶ Run su un telefono o un emulatore (Android 9+).

Senza configurare nulla parte in **modalità locale**: scrivi il tuo nome e i dati restano solo sul telefono.
Voce, animazioni, percorso a 6 domande, traguardi e check-in funzionano già.

## Attivare login Google, sync multi-dispositivo e Calendar (tutto gratis)

### 1. Firebase (piano Spark, gratuito)
1. Vai su [console.firebase.google.com](https://console.firebase.google.com) → **Aggiungi progetto**.
2. **Aggiungi app Android** con package `com.francescopaoli.northstar`.
3. Inserisci lo **SHA-1** della chiave di debug:
   ```
   ./gradlew signingReport
   ```
   (copia la riga `SHA1` della variante `debug`).
4. Scarica `google-services.json` e mettilo in `northstar/app/`.
   Non finisce su Git (è nel `.gitignore`).
5. **Authentication** → *Sign-in method* → attiva **Google** ed **Email/password**.
6. **Firestore Database** → *Crea database* → poi in *Regole* incolla il contenuto di `firestore.rules`.

Ricompila: il plugin Firebase si attiva da solo quando trova il file e l'app passa al login vero.

### 2. Google Calendar
Nel progetto Google Cloud creato da Firebase ([console.cloud.google.com](https://console.cloud.google.com)):
1. **API e servizi → Libreria** → attiva **Google Calendar API**.
2. **Schermata consenso OAuth** → aggiungi lo scope `.../auth/calendar.events`.
   Finché l'app è in modalità *Test*, aggiungi il tuo account tra gli **utenti di test**.

Il client OAuth Android lo crea già Firebase quando inserisci lo SHA-1.

### 3. Pubblicità (AdMob) e "Rimuovi pubblicità"
Solo **card native** nello stile dell'app, dentro le liste:
Home (dopo il 2° obiettivo), Traguardi (dopo il 3°), Dettaglio (in fondo).
**Mai** nel percorso a 6 domande, nella Celebrazione e nel Check-in. Niente banner, niente schermo intero.

In debug l'app usa sempre gli **annunci di test** di Google (non cliccare mai i tuoi annunci veri: AdMob banna l'account).

1. Crea un account su [admob.google.com](https://admob.google.com) → **Aggiungi app** (Android).
2. Crea un blocco annunci **Nativo avanzato**.
3. Metti gli ID in `~/.gradle/gradle.properties` (fuori dal repo):
   ```
   ADMOB_APP_ID=ca-app-pub-XXXXXXXX~YYYYYYYY
   ADMOB_NATIVE_ID=ca-app-pub-XXXXXXXX/ZZZZZZZZ
   ```
4. AdMob → **Privacy e messaggi** → crea il messaggio di consenso **GDPR** (in UE è obbligatorio; l'app lo mostra da sola al primo avvio).
5. Play Console → **Prodotti in-app** → crea il prodotto con ID `remove_ads` (una tantum, prezzo scelto: **3,99 €**).
   L'acquisto si prova solo con l'app caricata almeno su un canale di test e un account "tester licenze".

## Pubblicare su Play Store
1. Crea una chiave di firma (Android Studio → *Build → Generate Signed App Bundle*).
2. `./gradlew bundleRelease` → carica il `.aab` sulla Play Console.
3. Aggiungi su Firebase anche lo **SHA-1 di Play App Signing** (lo trovi nella Play Console), altrimenti il login Google non funziona nella versione scaricata dallo store.
4. Con la pubblicità servono una **privacy policy** (link nella scheda dello store) e il modulo **Sicurezza dei dati** che dichiara l'uso dell'ID pubblicità.
5. Per Calendar in produzione: la schermata di consenso va inviata a Google per la verifica (scope sensibile).

## Come è fatta

```
app/src/main/java/com/francescopaoli/northstar/
├── data/        Modello (Goal, Criterion…), Firestore / file locale, impostazioni
├── domain/      Frase riassuntiva, profilazione nascosta, regole dei check-in
├── auth/        Login Google (Credential Manager) + email, modalità locale
├── calendar/    Eventi su Google Calendar via REST
├── voice/       Voce guida (TTS) + ascolto con interruzione immediata
├── ads/         Consenso GDPR, AdMob, card native in stile neon
├── billing/     Acquisto una tantum "Rimuovi pubblicità"
├── notify/      Check-in e domanda "l'hai raggiunto?" (WorkManager, 1 volta al giorno)
└── ui/
    ├── fx/          Libreria animazioni (gradienti, glow, shimmer, particelle, coriandoli…)
    ├── components/  Bottoni, anelli di progresso, card, icone, barra in basso
    └── screens/     Le 8 schermate del mockup + Impostazioni
```

**Profilazione nascosta.** I suggerimenti dei criteri *Sotto il tuo controllo* ed *Ecologico* contengono
domande indirette ("le programmi nel dettaglio o le fai quando capita?"). `PersonalityProfiler` legge
lessico, livello di dettaglio e ripensamenti: chi ama controllare → eventi calendario **con conferma**,
chi delega → **automatici**. Sempre modificabile in Impostazioni.

**Interruzione vocale.** Mentre la guida parla, `BargeInDetector` ascolta il microfono con la
cancellazione d'eco del telefono: appena rileva la tua voce zittisce la guida e parte il riconoscimento.
Dipende dalla qualità dell'eco-cancellazione del dispositivo: con l'altoparlante a volume alto su
telefoni economici può scattare in ritardo o, raramente, da solo. Con le cuffie è perfetto.

## Comandi utili
```
./gradlew assembleDebug        # APK di prova
./gradlew testDebugUnitTest    # test della logica (frase riassuntiva, profilazione)
```

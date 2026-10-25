# WordSpark

WordSpark is an Android dictionary and vocabulary learning application developed using Java and XML in Android Studio.

The main idea behind the project is to provide a simple, clean interface where users can search for English words, view their meanings and related dictionary information, save useful words, review search history, and practice vocabulary using flashcards and quizzes.

## Features

- Local user registration, login, and forgot password support
- Dictionary search with live autocomplete suggestions and spelling recommendations
- Word details including meanings, definitions, parts of speech, pronunciation, synonyms, antonyms, examples, word forms, usage labels, and etymology
- Audio pronunciation playback and Read Definitions Aloud using Android Text-to-Speech (TTS)
- Saved words (Favorites) with live search filtering, Recent / A–Z sorting, and delete confirmation
- Search history tracking with relative timestamps and clear history support
- Vocabulary Lists across categories such as Technology & Computing, Business & Workplace, Academic Vocabulary, GRE Essentials, Everyday Idioms, and Advanced English
- Daily vocabulary rotation with 25 valid words per category per day
- Flashcards for vocabulary practice with review tracking
- Vocabulary quiz with 10 category-balanced questions per session and recent-question avoidance
- Word of the Day display on the Home screen and daily reminder notifications
- Light and Dark theme support
- SQLite-based local storage for user accounts, favorites, history, settings, and dictionary response caching

## Recent Improvements

- **Spelling Suggestions**: Displays clickable suggestion chips when an exact word match is not found.
- **Rich Word Details**: Parses and displays word forms/inflections, usage/domain labels (e.g., informal, technical), and etymology.
- **Favorites Management**: Added real-time search filtering and sorting (Recent vs. A–Z) within saved words.
- **Learning Progress Tracking**: Records quiz attempts, correct answers, best scores, and flashcard review counts in SQLite settings.
- **Offline Caching**: Successfully fetched dictionary responses are cached in local SQLite (`TABLE_DICTIONARY`) to allow previously viewed words to be accessed offline.

## Technologies Used

- Java
- XML
- Android Studio
- SQLite
- Merriam-Webster Learner's Dictionary API
- Merriam-Webster Thesaurus API
- JSON
- Android Text-to-Speech

## How the App Works

When a user searches for a word, WordSpark sends a request to the Merriam-Webster API and parses the JSON response to display the available dictionary information.

Dictionary data is fetched online when needed rather than storing a complete English dictionary locally inside the application. However, successfully fetched dictionary responses are cached in local SQLite so previously viewed words remain accessible offline.

User-specific data—such as user accounts, saved favorites, search history, progress statistics, and application settings—is stored locally using SQLite.

Learning features like vocabulary lists, flashcards, and the quiz question bank use local data pools so they remain accessible without making dictionary API requests for every card or question.

## Main Modules

### Search

Allows users to search for words with live autocomplete suggestions while typing. Handles empty input, word-not-found responses (with spelling suggestions), and network connectivity errors gracefully.

### Word Details

Displays information returned by the API for a selected word, including meanings, parts of speech, pronunciation, usage labels, word forms, etymology, synonyms, antonyms, and example sentences.

### Favorites

Users can save words to revisit them later. Includes real-time search filtering, sorting options (Recent and A–Z), and delete confirmation.

### Search History

Keeps track of previous searches along with their date and time. Users can tap a history entry to reopen the word or clear their history.

### Vocabulary Lists

Organized into categories (Technology & Computing, Business & Workplace, Academic Vocabulary, GRE Essentials, Everyday Idioms, Advanced English) with a daily vocabulary rotation.

### Flashcards

Allows users to practice vocabulary words in a card format with reveal interactions and Text-to-Speech reading.

### Quiz

Presents 10 questions per quiz session with four options per question drawn from a local question bank with recent-question tracking.

### Word of the Day

Displays a selected Word of the Day on the Home screen with daily notification reminders.

## Local Storage

SQLite is used for local data persistence:
- User accounts and authentication state
- Favorites / Saved words
- Search history
- User settings and learning progress (quiz stats and flashcard reviews)
- Local dictionary response cache (`TABLE_DICTIONARY`)

The application does not store a complete offline dictionary.

## API Configuration

WordSpark uses two Merriam-Webster APIs:
1. **Merriam-Webster Learner's Dictionary API**: Definitions, pronunciations, parts of speech, inflections, usage labels, etymology, and examples.
2. **Merriam-Webster Thesaurus API**: Synonyms and antonyms.

API keys are kept out of source control and configured locally in `local.properties`, which injects them into `BuildConfig`.

**Do not commit API keys to Git.**

## Requirements

- Android Studio
- Android SDK (minSdk 24, targetSdk 34)
- Java
- Internet connection for live dictionary API lookups

## Running the Project

1. Clone or download the repository.
2. Open the project folder in Android Studio.
3. Allow Gradle to sync.
4. Add your Merriam-Webster API keys to `local.properties`:
   ```properties
   MERRIAM_WEBSTER_LEARNER_API_KEY=your_learner_api_key_here
   MERRIAM_WEBSTER_THESAURUS_API_KEY=your_thesaurus_api_key_here
   ```
5. Build the project.
6. Run the app on an Android device or emulator.

## Testing & Build Commands

- **Unit Tests**: Run `./gradlew testDebugUnitTest` via Gradle.
- **Debug Build**: Run `./gradlew assembleDebug` via Gradle.
- **Debug APK Output Path**: `app/build/outputs/apk/debug/app-debug.apk` (or `app/build/outputs/apk/release/app-release-unsigned.apk` for release).

## Accessibility

WordSpark incorporates static accessibility improvements aligned with Android best practices and WCAG 2.2 AA guidelines:
- **Touch Targets**: Interactive controls maintain minimum bounding sizes of 48×48 dp with padding.
- **Contrast Ratios**: Foreground and background color combinations meet WCAG AA contrast thresholds (>= 4.5:1 for normal text, >= 3:1 for large text) in both Light and Dark modes.
- **Semantic Descriptions**: Informative icons have descriptive content descriptions while decorative graphics are excluded from accessibility traversal.
- **Text Resizing & Reflow**: Uses scalable units (`sp`/`dp`) and vertical `ScrollView` containers to support text scaling up to 200% and narrow viewports without content clipping.
- **Runtime Verification Note**: Live TalkBack screen reader navigation, hardware keyboard focus visibility, largest-font behavior, narrow-screen reflow, and dynamic status announcements require runtime device/emulator verification and are not claimed as fully verified via static checks. Certified WCAG 2.2 AA conformance is not claimed.

## Project Structure

```text
app/
└── src/
    └── main/
        ├── java/com/wordspark/app/
        │   ├── adapter/
        │   ├── db/
        │   ├── model/
        │   ├── network/
        │   ├── notification/
        │   ├── quiz/
        │   ├── util/
        │   └── vocab/
        │
        └── res/
            ├── drawable/
            ├── layout/
            ├── mipmap/
            └── values/
```

## Future Improvements

- Additional vocabulary categories and expanded question banks
- Personalized vocabulary recommendation algorithms
- Cloud-based user account synchronization across devices
- Additional language and translation support

## Notes

- Dictionary information depends entirely on the response provided by the Merriam-Webster API.
- Some fields (such as audio, synonyms, or examples) may not be available for every word.
- Fresh dictionary lookups require an active internet connection. Previously cached words may be viewed offline when present in the local database cache.
- All user account data, favorites, search history, and settings are stored locally on the device using SQLite.

## Team Members

WordSpark was developed as a team project as part of the
Mobile Application Development Lab.

- Priya Nayak
- Shetty Shiny Prashanth
- Abdul Majeed
- Ullas BH
=======
WordSpark is a Java-based Android dictionary application developed as a Mobile Application Development (MAD) course project. It features word search, dictionary API integration, Word of the Day, and vocabulary learning features. Developed collaboratively by a team of four students using Android Studio.
>>>>>>> origin/main

# Interactive Android App for Children

## Implementation of Voice Interaction for Application Control and Language Learning

## Concept

LingoPlay is an Android application designed as a **proof of concept** to explore the integration of voice-based technologies in educational tools, specifically designed for children.
The app incorporates two core functionalities: **voice commands** and **voice feedback (text-to-speech)**. These features enable users to interact with the app in a seamless and intuitive way, facilitating both **application control** and **active learning** of basic English vocabulary.
Voice commands provide an accessible and interactive experience, reducing the need for constant physical interaction with the device. This is especially beneficial for users with motor disabilities or young children who have not yet mastered reading and writing. At the same time, voice feedback enhances the learning experience by delivering clear instructions, encouragement, and guidance throughout the game.

The implementation of both functionalities demonstrates the **potential of voice technologies** to improve accessibility, interactivity, and user engagement in educational processes. In a more extensive version, the app could include additional thematic levels and difficulty grades, opening up new possibilities for personalized and adaptive language learning.

This project emphasizes the significance and impact of technological innovation in modern education.

## Technologies Used

- **Programming Language:** Kotlin
- **IDE:** Android Studio
- **Speech Recognition:** Google Speech Recognizer API
- **Text-to-Speech:** Android Text-to-Speech
- **UI Components:** ConstraintLayout, Buttons, ImageViews, and ProgressBar
- **Coroutine Management:** Kotlin Coroutines for smooth asynchronous operations

## Core functionalities

1. **Voice Commands:** Used for easy navigation and interaction within the application. Users can give commands like "Play" or "Back" to start or navigate through levels. Additionally, voice commands are part of the learning activities, where users repeat English words, such as numbers or colors, to enhance vocabulary.

2. **Voice Feedback:** Provides real-time guidance and encouragement via text-to-speech (TTS), ensuring an engaging learning experience. Instructions and prompts are delivered in a child-friendly maner.

3. **Flexible Input Methods:** To ensure complete accessibility and adaptability, the application allows users to manage interactions using both voice commands and touch-based buttons. This design ensures that users can choose the method most comfortable or suitable for their needs, making the application inclusive for a diverse range of users.

4. **Interactive Thematic Levels:** Includes two levels focusing on learning basic English vocabulary:

   - **Colors Level:** Teaches basic color names in English through an engaging, repeat-after-me game.
   - **Numbers Level:** This Activity is analogous to the Colors Activity, sharing a similar structure and functionality, with the focus shifted to teaching basic numbers in English.

5. **Accessibility and Adaptability:** Designed for children and users with motor disabilities, making the learning process more inclusive.

6. **Replay and Navigation:** Users can replay levels or navigate back to the main menu easily.

7. **Dynamic Speech Recognition:** Employs flexible word-matching to handle variations in pronunciation and accents.

8. **Error handling:** Controlled delays to synchronize TTS and Speech Recognizer.
   Toast messages for user guidance on errors.

9. **UI Interaction**
   Play and Back buttons for touch-based navigation.
   Visual cues (e.g., number and color images) for enhanced engagement.
   Toast messages for feedback and encouragement.

## Setup and Installation

1. Clone the repository:

```bash
git clone  https://github.com/vlasdana/Projektarbeit.git`
```

2. Open the project in **Android Studio.**
3. Sync the Gradle files to install dependencies.
4. Connect a physical device or start an Android emulator. (physical device recomended)
5. Run the application using the **Run button** in Android Studio.

## App structure

### MainActivity

The MainActivity serves as the starting screen of the application. It features the game’s avatar, which interacts with the user through engaging prompts, inviting him to begin the educational experience.

Users are provided with two options to proceed:

- **Voice Command:** By speaking the appropriate command, users can interact vocally with the application.
- **Play Button:** A dedicated button allows users to advance further into the application through touch, making the use of voice commands optional.

From here, users can navigate to the Language Selection Page to choose their native language.

### LanguageActivity

This activity allows users to select their native language, which will be used to manage the application.

### ChoiceActivity

Provides options for user interaction modes (voice or touch) and presents users the main commands for managing the application. (Play and Back)

### LevelsActivity

The LevelsActivity acts as a hub, presenting available levels (Colors and Numbers). Users can navigate between levels based on their interests.

### ColorsActivity

This activity focuses on teaching basic color names in English. Users are presented with visual cues (colored balls) and voice prompts in both German and English. The interaction involves repeating the names of the colors, improving both pronunciation and vocabulary.

### NumbersActivity

Analogous to the ColorsActivity, this level teaches the user numbers from zero to ten in English. Voice prompts and visual elements guide the user, encouraging active participation and reinforcing language skills through repetition.

## Implementation & Key Methods

- Initiate voice recognition for numbers/colors

```Kotlin
startListeningForNumber(expectedNumber: String)
startListeningForColor(expectedColor: String)
```

- Validate user input with flexible pronunciation mapping.

```Kotlin
isNumberMatch(spokenText: String, expectedNumber: String)
isColorMatch(spokenText: String, expectedColor: String)
```

- Provides TTS feedback in English/German

```Kotlin
 speakInEnglish(text: String, utteranceId: String)
 speakInGerman(text: String)
```

- Resets game state and restarts the learning session.

```Kotlin
resetGameAndStart()
```

### Kotlin Coroutines

Kotlin Coroutines are crucial for efficiently handling asynchronous tasks in this application. They ensure a responsive user experience by managing Text-to-Speech (TTS), Speech Recognition, and UI updates with lightweight, structured concurrency, offering a simpler and more efficient alternative to traditional threading.

`coroutineScope.launch` - used to launch a non-blocking coroutine within a coroutine context (e.g., Dispatchers.Main, Dispatchers.IO).

`delay()` - function to suspend the execution of a coroutine without blocking the thread.

```Kotlin
coroutineScope.launch {
   speakInGerman("Ich werde dir eine Zahl sagen, und du musst sie wiederholen.")
   delay(1500)
   presentNumber()
}
```

Coroutines are canceled when the activity is destroyed, preventing memory leaks and ensuring smooth lifecycle management.

`coroutineScope.cancel` - used to cancel all coroutines running within a specific scope. It ensures that any ongoing asynchronous tasks, such as Text-to-Speech or Speech Recognition operations, are properly terminated when the activity is destroyed.

Example in `onDestroy()`

```Kotlin
override fun onDestroy() {
   stopTTS()
   speechRecognizer.cancel()
   speechRecognizer.destroy()
   coroutineScope.cancel()
   super.onDestroy()
}
```

## Challenges and Solutions

1. **Speech Recognition Accuracy**

**Problem:** Difficulty recognizing accents and pronunciation variations.

**Solution:** Implemented flexible mapping functions for multiple pronunciation variants.

2. **TTS and Speech Recognizer Synchronization**

**Problem:** Short delays disrupted the smooth interaction flow.

**Solution:** Added controlled delays and lifecycle management for better synchronization.

- Despite the implemented solution, certain words, particularly short single-syllable ones like "two" or "six," are sometimes not correctly identified by the Speech Recognizer. To mitigate this issue, it is recommended to pronounce the word along with an adjacent phrase, such as "number two" or "this is six," to improve recognition accuracy.

3. **Simultaneous Inputs**

**Problem:** Rapid button presses caused duplicate game instances.

**Solution:** Added a state flag `(isGameRunning)` to prevent this.

4. **Voice Selection**

**Problem:** Android's built-in Text-to-Speech (TTS) does not offer child-friendly voices, limiting the ability to create an engaging, age-appropriate experience for younger users.

**Potential Solution:** Paid external TTS services like Amazon Polly, Microsoft Azure, or Picovoice.

**Reason Not Implemented:** Due to the paid nature of these solutions, they were not integrated into the current proof-of-concept application. However, these technologies remain viable options for future development or scaling of the project.

## Milestones

- **Initial Design:**
  - Planned app structure and defined key learning objectives.
  - Created a prototype in Figma to visualize the user interface and user flow.
  - Designed initial layouts using ConstraintLayout.
- **Voice Command Implementation:**
  - Integrated Google Speech Recognizer API for real-time user interaction.
  - Developed methods to handle voice input with flexible error tolerance.
- **Text-to-Speech Feedback:**
- - Added natural-sounding feedback using Android TTS.
  - Enabled bilingual support for English and German.
- **Error Handling:**
  - Implemented mapping functions (isColorMatch, isNumberMatch) to handle pronunciation variations.
  - Managed delays and lifecycle synchronization between TTS and Speech Recognizer.
- **Coroutine and Thread Management**
  - Utilized Kotlin Coroutines to handle asynchronous tasks like TTS, Speech Recognition, and UI updates.
  - Implemented delay() for smooth task execution and lifecycle management.
  - Ensured proper resource cleanup with CoroutineScope.cancel in the onDestroy method.
- **User Interaction Refinement:**
  - Ensured smooth transitions between replay prompts and game start.
  - Added prevention for multiple game instances on rapid button presses.
- **Final Testing and Optimization:**
  - Adjusted TTS speed and added compatibility checks for different devices.
  - Conducted real-device testing for accurate performance validation.

## Future Improvements

**Expanded Levels:** Add more levels with varied themes (e.g., shapes, animals, and objects).

**Enhanced Feedback:** Integrate dynamic voice customization for user-specific preferences.

**Offline Support:** Use technologies like Vosk or Picovoice for offline voice recognition.

**Personalization:** Introduce user profiles for progress tracking and difficulty adjustment.

**Haptic Feedback:** Provide vibrations or visual effects for users with hearing impairments.

# Interactive Android App for Children

## Implementation of Voice Interaction for Application Control and Language Learning

### Concept

LingoPlay is an Android application designed as a **proof of concept** to explore the integration of voice-based technologies in educational tools, specifically designed for children.
The app incorporates two core functionalities: **voice commands** and **voice feedback (text-to-speech)**. These features enable users to interact with the app in a seamless and intuitive way, facilitating both **application control** and **active learning** of basic English vocabulary.
Voice commands provide an accessible and interactive experience, reducing the need for constant physical interaction with the device. This is especially beneficial for users with motor disabilities or young children who have not yet mastered reading and writing. At the same time, voice feedback enhances the learning experience by delivering clear instructions, encouragement, and guidance throughout the game.

The implementation of both functionalities demonstrates the **potential of voice technologies** to improve accessibility, interactivity, and user engagement in educational processes. In a more extensive version, the app could include additional thematic levels and difficulty grades, opening up new possibilities for personalized and adaptive language learning.

This project emphasizes the significance and impact of technological innovation in modern education.

### Technologies Used

- **Programming Language:** Kotlin
- **IDE:** Android Studio
- **Speech Recognition:** Google Speech Recognizer API
- **Text-to-Speech:** Android Text-to-Speech
- **UI Components:** ConstraintLayout, Buttons, ImageViews, and ProgressBar
- **Coroutine Management:** Kotlin Coroutines for smooth asynchronous operations

### Core functionalities

**Voice Commands:** Used for easy navigation and interaction within the application. Users can give commands like "Play" or "Back" to start or navigate through levels. Additionally, voice commands are part of the learning activities, where users repeat English words, such as numbers or colors, to enhance vocabulary.

**Voice Feedback:** Provides real-time guidance and encouragement via text-to-speech (TTS), ensuring an engaging learning experience. Instructions and prompts are delivered in a child-friendly maner.

**Interactive Thematic Levels:** Includes two levels focusing on learning basic English vocabulary:

- **Colors Level:** Teaches basic color names in English through an engaging, repeat-after-me game.
- **Numbers Level:** This Activity is analogous to the Colors Activity, sharing a similar structure and functionality, with the focus shifted to teaching basic numbers in English.

**Accessibility and Adaptability:** Designed for children and users with motor disabilities, making the learning process more inclusive.

**Replay and Navigation:** Users can replay levels or navigate back to the main menu easily.

**Dynamic Speech Recognition:** Employs flexible word-matching to handle variations in pronunciation and accents.

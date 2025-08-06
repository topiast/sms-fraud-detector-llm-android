# SMS Fraud Detector – AI Context Documentation

## Project Overview
**SMS Fraud Detector** is a privacy-focused Android application that scans incoming SMS messages for fraud using both simple rules and on-device AI models (LLMs). All detection and analysis are performed locally, with no network required for inference. The app provides a guided onboarding flow for new users, real-time notifications, persistent message history, and advanced model management.

## Purpose & Core Functionality
- **Onboarding flow**: Guides new users through permissions, privacy, and app features on first launch
- **Real-time SMS monitoring**: Intercepts and analyzes all incoming SMS messages
- **Fraud detection**: Supports both keyword-based and LLM-powered detection (local inference)
- **Instant notifications**: Clickable notifications for fraud warnings and safe confirmations
- **Message history**: Persistent storage of all analyzed messages with detailed reports
- **User management**: Flag, delete, and report suspicious messages
- **Model management**: Download, configure, and debug local LLMs in-app
- **Text sharing analysis**: Users can share arbitrary text (e.g., from other apps) to the app for fraud analysis via Android's native share feature

## Architecture & Technology Stack

### Frontend
- **Framework**: Jetpack Compose (Material Design 3)
- **Language**: Kotlin
- **UI Pattern**: Declarative UI with reactive state management, onboarding navigation, and animated transitions
- **Navigation**: Intent-based activity navigation, onboarding flow on first launch

### Backend/Data Layer
- **Database**: Room Database (SQLite wrapper)
- **Storage Pattern**: Repository pattern with DAO layer
- **Async**: Kotlin Coroutines with Flow for reactive data streams
- **Serialization**: Gson for JSON handling

### LLM Integration
- **Model Management**: Download and manage local LLMs (e.g., HuggingFace)
- **Config & Tokens**: In-app configuration for model selection and API tokens
- **Inference**: On-device fraud detection using LLMs (no cloud required)
- **Debug UI**: Dedicated screens for testing and debugging model responses

### System Integration
- **SMS Interception**: BroadcastReceiver for `SMS_RECEIVED_ACTION`
- **Background Processing**: `ForegroundService` for durable, cancellable analysis.
- **Notifications**: NotificationManager with clickable PendingIntents
- **Permissions**: Runtime permission handling for SMS and notifications
- **Text Sharing**: Intent filter for `android.intent.action.SEND` with `text/plain` to receive shared text from other apps

## File Structure & Components

### Onboarding Flow
```
app/src/main/java/com/example/smsfrauddetector/onboarding/
├── OnboardingActivity.kt        # Entry point for onboarding flow
├── OnboardingViewModel.kt       # State management and navigation for onboarding
├── models/OnboardingPage.kt     # Data model for onboarding pages
├── screens/WelcomeScreen.kt     # Welcome and introduction screen
├── screens/PermissionsScreen.kt # Permissions request and explanation
├── screens/PrivacyScreen.kt     # Privacy information screen
├── screens/AIDetectionScreen.kt # AI detection explanation screen
├── screens/HistoryScreen.kt     # History feature introduction
├── components/PageIndicator.kt  # Progress indicator for onboarding pages
├── components/OnboardingNavigation.kt # Navigation controls for onboarding flow
```

### Core Activities & Services
```
app/src/main/java/com/example/smsfrauddetector/
├── MainActivity.kt              # Main screen with permissions & navigation
├── SummaryActivity.kt           # Detailed message analysis view
├── HistoryActivity.kt           # List of all processed messages
├── ModelDownloadActivity.kt     # LLM model download and management UI
├── LlmDebugActivity.kt          # LLM inference debug/testing UI
├── SmsReceiver.kt               # SMS broadcast receiver
├── NotificationService.kt       # Notification management
├── FraudDetector.kt             # Fraud analysis logic (rules + LLM)
├── FraudDetectionService.kt     # Foreground service for durable, cancellable fraud analysis
├── ShareReceiverActivity.kt     # Handles text shared from other apps, confirmation UI, and triggers analysis
```

### LLM Integration & Utilities
```
app/src/main/java/com/example/smsfrauddetector/llm/
├── Config.kt                    # LLM configuration, enums, and settings
├── DownloadRepository.kt        # DownloadRepository interface & DefaultDownloadRepository implementation
├── DownloadWorker.kt            # Background worker for model downloads
├── HuggingFaceTokenManager.kt   # Token management for HuggingFace
├── LlmInferenceHelper.kt        # LLM inference logic (init, generate, cleanup)
├── LlmUtils.kt                  # Utility functions (prompt building, accelerator enum, chat configs)
├── Model.kt                     # Model metadata & ModelRegistry
├── ModelDownloadStatus.kt       # Download status tracking
├── ModelManagerViewModel.kt     # ViewModel for model management
└── ViewModelFactory.kt          # Factory for ViewModels
```

### Database Layer
```
app/src/main/java/com/example/smsfrauddetector/database/
├── SmsReport.kt                # Entity model for message reports, helpers, companion object
├── SmsReportDao.kt             # Data access object with queries
├── AppDatabase.kt              # Room database config, singleton, migration logic
└── SmsReportRepository.kt      # Repository pattern implementation, singleton access
```

### UI Theme & Typography
```
app/src/main/java/com/example/smsfrauddetector/ui/theme/
├── Color.kt                    # Color definitions for Material Design 3
├── Theme.kt                    # Light/Dark color schemes, theme composable
└── Type.kt                     # Typography definitions
```

### Configuration & Resources
```
app/
├── build.gradle.kts             # Dependencies (Room, Compose, Gson, LLM)
├── src/main/AndroidManifest.xml # App permissions & activity declarations
└── src/main/res/                # UI resources, drawables, themes, and mipmaps
```

## Key Classes & Responsibilities

### OnboardingActivity.kt
- **Purpose**: Entry point for the onboarding flow, shown on first launch
- **Key Features**:
  - Hosts onboarding screens and manages navigation
  - Handles completion of onboarding and transition to main app

### OnboardingViewModel.kt
- **Purpose**: State management and navigation for onboarding
- **Key Features**:
  - Tracks current onboarding page
  - Provides navigation actions (next, previous, skip)
  - Stores onboarding completion state

### Onboarding Screens
- **WelcomeScreen.kt**: Introduction to app features and purpose
- **PermissionsScreen.kt**: Requests and explains required permissions
- **PrivacyScreen.kt**: Presents privacy information and local processing
- **AIDetectionScreen.kt**: Explains AI-powered fraud detection
- **HistoryScreen.kt**: Introduces message history and reporting

### Onboarding Components
- **PageIndicator.kt**: Visual progress indicator for onboarding pages
- **OnboardingNavigation.kt**: Navigation controls (next, previous, skip)

### MainActivity.kt
- **Purpose**: Entry point with permission management
- **Key Features**:
  - SMS and notification permission requests
  - App status display
  - Navigation to history and model management screens

### ModelDownloadActivity.kt
- **Purpose**: Download and manage local LLMs
- **Key Features**:
  - List available models
  - Download, update, or remove models
  - Configure model settings and tokens

### LlmDebugActivity.kt
- **Purpose**: Test and debug LLM inference
- **Key Features**:
  - Input custom SMS text and view model analysis
  - Compare rule-based and LLM-based results
  - Useful for development and QA

### SummaryActivity.kt
- **Purpose**: Detailed view of individual message analysis
- **Key Features**:
  - Fraud probability display with visual indicators
  - Text highlighting of suspicious words (red background)
  - Manual flagging toggle
  - Delete message functionality
  - Report button (placeholder with toast)
  - **Start/Stop Controls**: Allows the user to cancel and restart the analysis of a message.
- **Data Flow**: Observes an `SmsReport` from the database as a `Flow`.
- **Reactive UI**: The UI automatically updates based on the `processingStatus` of the report (`processing`, `stopped`, `processed`).

### HistoryActivity.kt
- **Purpose**: List view of all processed messages
- **Key Features**:
  - Filter tabs (All, Fraud, Safe, Flagged)
  - Clickable list items opening SummaryActivity
  - Empty state handling
  - Real-time data updates via Flow
  - Displays the processing status of each message (`processing`, `stopped`, `processed`).
- **UI Pattern**: LazyColumn with filterable data

### ShareReceiverActivity.kt
- **Purpose**: Entry point for Android's native share feature (text sharing)
- **Key Features**:
  - Receives shared text from other apps via `android.intent.action.SEND`
  - Displays the shared text and asks for user confirmation to analyze
  - On confirmation, creates a new `SmsReport` and starts `FraudDetectionService` for analysis
  - Navigates to `SummaryActivity` to show analysis results
- **Integration**: Registered in `AndroidManifest.xml` with an intent filter for `text/plain` sharing

### SmsReceiver.kt
- **Purpose**: Intercepts incoming SMS messages
- **Process Flow**:
  1. Receives SMS broadcast.
  2. Extracts message content and sender.
  3. Creates an initial `SmsReport` entity in the database with `processingStatus = "processing"`.
  4. Starts the `FraudDetectionService` with an intent containing the new report's ID.
- **Threading**: Uses a coroutine to insert the initial report into the database.

### FraudDetector.kt
- **Purpose**: Core fraud detection logic, called by the `FraudDetectionService`.
- **Current Algorithm**: Keyword detection and/or LLM inference
- **Output**: FraudAnalysisResult with probability, suspicious words, reasons
- **Extensibility**: Designed for ML model integration and rule expansion

### NotificationService.kt
- **Purpose**: Manages system notifications
- **Features**:
  - Separate channels for fraud/safe messages.
  - Clickable notifications with `PendingIntent`s.
  - **Cancel Action**: Adds a "Cancel" action to notifications for messages currently being processed.
  - Priority and vibration handling.
  - Permission checking.
- **Integration**: Receives report ID to create clickable actions.

## Database Schema

### SmsReport Entity
```kotlin
@Entity(tableName = "sms_reports")
data class SmsReport(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val sender: String?,
    val messageBody: String,
    val isFraud: Boolean,
    val fraudProbability: Double,
    val suspiciousWords: String,        // JSON array
    val llmReason: String? = null,      // LLM explanation/reason
    val isManuallyFlagged: Boolean,
    val isDeleted: Boolean,             // Soft delete
    val processingStatus: String        // "processing", "processed", or "stopped"
)
```

### Key Queries
- Get all reports (filtered, sorted by timestamp)
- Search by message content
- Update manual flag status
- Soft delete (isDeleted = true)
- Count statistics

## Data Flow Architecture

```
SMS Message → SmsReceiver → Creates initial SmsReport (status: "processing") → Starts Service
                                                                                     ↓
                                                                          FraudDetectionService
                                                                                     ↓
                                                                          (Runs FraudDetector)
                                                                                     ↓
                                                                          Updates SmsReport (status: "processed")
                                                                                     ↓
                                                                          NotificationService → User

Shared Text → ShareReceiverActivity → Shows confirmation UI → On confirm: creates SmsReport (status: "processing") → Starts Service
                                                                                     ↓
                                                                          FraudDetectionService
                                                                                     ↓
                                                                          (Runs FraudDetector)
                                                                                     ↓
                                                                          Updates SmsReport (status: "processed")
                                                                                     ↓
                                                                          NotificationService → User
```

## Permissions & Security
- **SMS Permissions**: `READ_SMS`, `RECEIVE_SMS`
- **Notification Permission**: `POST_NOTIFICATIONS` (Android 13+)
- **Privacy**: All processing is local, no network access required for detection
- **Security**: No sensitive data exposure, local storage only

## UI/UX Patterns

### Onboarding Experience
- **First-launch onboarding flow**: Guides users through permissions, privacy, AI detection, and app features
- **Animated transitions**: Smooth navigation between onboarding screens
- **Progress indicators**: Visual feedback for onboarding completion
- **Material Design 3**: Consistent theming and components across onboarding and main app

### Material Design 3
- **Color Scheme**: Dynamic theming with fraud/safe color coding
- **Components**: Cards, buttons, progress indicators, chips
- **Typography**: Consistent text styles with emphasis hierarchy

### State Management
- **Reactive**: Compose state with Flow data streams and onboarding navigation
- **Loading States**: Proper loading indicators and error handling
- **Real-time Updates**: The UI automatically updates based on changes in the database via Kotlin Flow.

### Navigation
- **Pattern**: Activity-based with intent extras and onboarding flow on first launch
- **Back Stack**: Proper parent activity relationships
- **Deep Links**: Notification → SummaryActivity with report ID
- **Share Integration**: ShareReceiverActivity can be launched from other apps via the Android share sheet

## Extensibility Points

### Fraud Detection
- Swap between rule-based and LLM-powered detection
- Add new ML models or pattern recognition algorithms
- Integrate external fraud databases
- Implement sender verification

### LLM & Model Management
- Add support for new model formats or providers
- Extend debug UI for advanced testing
- Enhance model download and update flows

### UI Enhancements
- Add dark mode support
- Implement advanced filtering
- Add export functionality
- Create detailed statistics

### Data Management
- Add backup/restore functionality
- Implement data encryption
- Add bulk operations
- Create data analytics

## Development Notes

### Build System
- **Gradle**: Kotlin DSL with version catalogs
- **Dependencies**: Room, Compose BOM, Material 3, Gson, LLM libraries
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **Debugging**: Testing is manual and handled by the user (AI should not try to automatically test)

### Testing Strategy
- **Unit Tests**: FraudDetector logic (rules + LLM)
- **Integration Tests**: Database operations
- **UI Tests**: Compose testing with semantics
- **Manual QA**: Use LLM Debug screen for inference validation

### Performance Considerations
- **Database**: Indexed queries for performance
- **UI**: Lazy loading for large lists
- **Memory**: Efficient state management
- **Background**: Coroutines for non-blocking operations
- **LLM**: On-device inference optimized for mobile

## Common Development Tasks

### Adding New Fraud Detection Rules or Models
1. Update `FraudDetector.analyzeMessage()` for rules or LLM logic
2. Modify suspicious words detection
3. Update UI highlighting logic
4. Test with various message types and LLM debug screen

### Adding New UI Screens
1. Create new Activity with Compose UI
2. Add to AndroidManifest.xml
3. Implement navigation from existing screens
4. Add repository integration for data

### Database Schema Changes
1. Update entity models
2. Create migration scripts
3. Increment database version
4. Test migration paths

This documentation provides a comprehensive, up-to-date overview for AI models and developers to quickly understand the project structure, purpose, and implementation details.

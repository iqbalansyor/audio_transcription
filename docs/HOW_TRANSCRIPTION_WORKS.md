# How Transcription Works

A simple explanation of what happens when you tap "Transcribe".

## Overview

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Your Voice │ -> │  WAV File   │ -> │   Whisper   │ -> │    Text     │
│  (Sound)    │    │  (Numbers)  │    │   Model     │    │   Output    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

## Step 1: Recording (Sound → Numbers)

When you speak into the microphone:
- Your voice creates **sound waves** (air vibrations)
- The microphone converts these vibrations into **electrical signals**
- The app samples this signal **16,000 times per second** (16kHz)
- Each sample becomes a **number** between -32,767 and +32,767
- These numbers are saved as a **WAV file**

```
Your voice: "Hello"
            ↓
WAV file:   [0, 150, 890, 2340, 5670, 8901, 12340, ...]
            (thousands of numbers)
```

## Step 2: Normalize (Prepare for Model)

The WAV numbers are converted to **float values** between -1.0 and +1.0:

```
[0, 150, 890, 2340, ...]  →  [0.0, 0.005, 0.027, 0.071, ...]
```

## Step 3: Whisper Model (Numbers → Text)

This is where the "magic" happens. The Whisper model is a **neural network** - like a brain made of math:

```
Audio Numbers → [Neural Network with millions of parameters] → Text
```

### Inside the model:

1. **Feature Extraction**: Converts raw audio into patterns (like a spectrogram - visual representation of sound frequencies over time)

2. **Encoder**: Analyzes the audio patterns and creates a "summary" of what it heard (called embeddings)

3. **Decoder**: Takes the summary and predicts text, one word at a time:
   ```
   Summary → "I" → "know" → "we" → "can" → "do" → "this" → ...
   ```

## Why Does It Take Time?

The `ggml-tiny` model has **~39 million parameters** (numbers). During transcription:

- Every audio sample must pass through all these parameters
- Millions of mathematical operations (multiply, add) happen
- On a phone CPU, this takes time

```
4 sec audio × 16,000 samples/sec = 64,000 samples
64,000 samples × millions of operations = takes time!
```

## Simple Analogy

Think of it like translating a foreign language:

| Step | Analogy |
|------|---------|
| **Recording** | Writing down what someone said in a foreign script |
| **Normalize** | Converting the script to a standard format |
| **Whisper Model** | A translator who looks at each sound and writes out the meaning |

The model "learned" these patterns by listening to **680,000 hours** of audio with matching text during training!

## Code Flow

```kotlin
// 1. Read WAV file
val audioData = decodeWaveFile(file)  // [0.01, -0.02, 0.05, ...]

// 2. Send to Whisper model
val result = whisperContext.transcribeData(audioData)

// 3. Get text back
// result = "I know we can do this together"
```

## Summary

| Stage | Input | Output |
|-------|-------|--------|
| Record | Sound waves | WAV file (numbers) |
| Normalize | WAV integers | Float array (-1 to 1) |
| Transcribe | Float array | Text string |

That's it! **Sound waves → Numbers → Neural network → Text.**

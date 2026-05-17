import java.io.File
import java.util.concurrent.TimeUnit

fun runCommand(command: String, workingDir: File): Boolean {
    return try {
        val process = ProcessBuilder("bash", "-c", command)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        // Bumping timeout to 10 minutes because Demucs is a heavy deep-learning model
        val finished = process.waitFor(10, TimeUnit.MINUTES)
        if (!finished) {
            process.destroy()
            println("❌ Error: Process timed out!")
            return false
        }
        process.exitValue() == 0
    } catch (e: Exception) {
        println("❌ System Error: ${e.message}")
        false
    }
}

fun main() {
    val workDir = File(System.getProperty("user.dir"))

    val originalAudio = "song.mp3"
    // Demucs automatically creates this specific folder structure when it finishes
    val isolatedMelody = "separated/htdemucs/song/vocals.wav"
    val midiFile = "vocals_basic_pitch.mid"

    println("🎵 Starting Cloud Transcription Pipeline (Advanced Stem-Splitter Mode)...")

    if (!File(workDir, originalAudio).exists()) {
        println("❌ Error: 'song.mp3' not found in the project root!")
        return
    }

    println("\n▶ Step 1: Running Demucs AI (Ripping background music away from the melody)...")
    // --two-stems=vocals tells the AI to only care about isolating the main vocal/melody line
    val demucsSuccess = runCommand("demucs --two-stems=vocals $originalAudio", workDir)
    if (!demucsSuccess) { println("Demucs processing failed. Exiting."); return }

    if (!File(workDir, isolatedMelody).exists()) {
        println("❌ Error: Isolated vocal file not found where expected.")
        return
    }

    println("\n▶ Step 2: AI converting isolated melody to MIDI...")
    val aiSuccess = runCommand("basic-pitch ./ $isolatedMelody", workDir)
    if (!aiSuccess) { println("AI processing failed. Exiting."); return }

    println("\n▶ Step 3: Generating Solo PDF Sheet Music...")
    val pdfSuccess = runCommand("mscore $midiFile -o FinalSheetMusic.pdf", workDir)
    if (!pdfSuccess) { println("PDF generation failed. Exiting."); return }

    println("\n✅ Success! Clean FinalSheetMusic.pdf generated successfully.")
}
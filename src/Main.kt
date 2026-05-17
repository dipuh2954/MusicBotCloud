import java.io.File
import java.util.concurrent.TimeUnit

fun runCommand(command: String, workingDir: File): Boolean {
    return try {
        val process = ProcessBuilder("bash", "-c", command)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val finished = process.waitFor(5, TimeUnit.MINUTES)
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

// Notice we removed the 'args' requirement since we don't need a URL anymore!
fun main() {
    val workDir = File(System.getProperty("user.dir"))

    val audioFile = "song.mp3"
    val midiFile = "song_basic_pitch.mid"

    println("🎵 Starting Cloud Transcription Pipeline (Manual Upload Mode)...")

    // We check to make sure you actually put the file in the right place
    if (!File(workDir, audioFile).exists()) {
        println("❌ Error: 'song.mp3' not found in the project root! Please add it and try again.")
        return
    }

    println("\n▶ Step 1: Skipping Download (Using provided song.mp3)...")

    println("\n▶ Step 2: AI converting to MIDI (This may take a minute)...")
    val aiSuccess = runCommand("basic-pitch ./ $audioFile", workDir)
    if (!aiSuccess) { println("AI processing failed. Exiting."); return }

    println("\n▶ Step 3: Generating PDF Sheet Music...")
    val pdfSuccess = runCommand("mscore $midiFile -o FinalSheetMusic.pdf", workDir)
    if (!pdfSuccess) { println("PDF generation failed. Exiting."); return }

    println("\n✅ Success! FinalSheetMusic.pdf generated successfully.")
}
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

// This function safely runs terminal commands and handles crashes
fun runCommand(command: String, workingDir: File): Boolean {
    return try {
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT) // Shows tool output in your IntelliJ console
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        // Give the AI up to 5 minutes to process, then kill it if it freezes
        val finished = process.waitFor(5, TimeUnit.MINUTES)
        if (!finished) {
            process.destroy()
            println("❌ Error: Process timed out!")
            return false
        }

        // Exit code 0 means the tool finished successfully
        process.exitValue() == 0
    } catch (e: Exception) {
        println("❌ System Error: Could not run command. Is the tool installed? Details: ${e.message}")
        false
    }
}

fun main(args: Array<String>) {
    // 1. Check if you provided a YouTube link
    if (args.isEmpty()) {
        println("❌ Error: Please provide a YouTube link as an argument.")
        exitProcess(1)
    }

    val youtubeLink = args[0]
    val workDir = File(System.getProperty("user.dir"))

    // The temporary files we need during the process
    val audioFile = "song.mp3"
    val midiFile = "song_basic_pitch.mid"

    println("🎵 Starting Transcription Pipeline...")
    println("📂 Working Directory: ${workDir.absolutePath}")

    // Step 1: Download
    println("\n▶ Step 1: Downloading Audio from YouTube...")
    val dlSuccess = runCommand("yt-dlp -x --audio-format mp3 -o $audioFile $youtubeLink", workDir)
    if (!dlSuccess) { println("Failed to download audio. Exiting."); return }

    // Step 2: AI Processing
    println("\n▶ Step 2: AI converting to MIDI (This may take a minute)...")
    val aiSuccess = runCommand("basic-pitch ./ $audioFile", workDir)
    if (!aiSuccess) { println("AI processing failed. Exiting."); return }

    // Step 3: Engraving the Sheet Music
    println("\n▶ Step 3: Generating PDF Sheet Music...")
    val pdfSuccess = runCommand("mscore $midiFile -o FinalSheetMusic.pdf", workDir)
    if (!pdfSuccess) { println("PDF generation failed. Exiting."); return }

    // Step 4: Clean up the mess
    println("\n▶ Step 4: Cleaning up temporary files...")
    File(workDir, audioFile).delete()
    File(workDir, midiFile).delete()

    println("\n✅ Success! Your sheet music is ready.")
    println("📄 Find it here: ${workDir.absolutePath}/FinalSheetMusic.pdf")
}
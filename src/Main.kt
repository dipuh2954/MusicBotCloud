import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun runCommand(command: String, workingDir: File): Boolean {
    return try {
        // In Linux/Cloud environments, it's safer to pass the command to the bash shell directly
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

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("❌ Error: Please provide a YouTube link as an argument.")
        exitProcess(1)
    }

    val youtubeLink = args[0]
    val workDir = File(System.getProperty("user.dir"))

    val audioFile = "song.mp3"
    val midiFile = "song_basic_pitch.mid"

    println("🎵 Starting Cloud Transcription Pipeline...")

    println("\n▶ Step 1: Downloading Audio from YouTube (Using Android Spoof Bypass)...")
    // We added the --extractor-args flag here to trick YouTube into thinking this is a mobile phone
    val dlSuccess = runCommand("yt-dlp --extractor-args \"youtube:player_client=android\" -x --audio-format mp3 -o $audioFile \"$youtubeLink\"", workDir)
    if (!dlSuccess) { println("Failed to download audio. Exiting."); return }

    println("\n▶ Step 2: AI converting to MIDI...")
    val aiSuccess = runCommand("basic-pitch ./ $audioFile", workDir)
    if (!aiSuccess) { println("AI processing failed. Exiting."); return }

    println("\n▶ Step 3: Generating PDF Sheet Music...")
    // Using the Linux mscore command without a GUI display
    val pdfSuccess = runCommand("mscore $midiFile -o FinalSheetMusic.pdf", workDir)
    if (!pdfSuccess) { println("PDF generation failed. Exiting."); return }

    println("\n✅ Success! FinalSheetMusic.pdf generated successfully.")
}
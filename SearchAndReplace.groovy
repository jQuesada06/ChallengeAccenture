import groovy.io.FileType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Description of SearchAndReplace.
 *
 * The SearchAndReplace class performs the following functions:
 * - Searches for a given pattern in text files in a directory and the subdirectories present and modifies it to another given pattern.
 * - Backs up files before modifying them.
 * - Generate a txt file with the log of the modified files and in which line they were modified.
 *
 * The parameters are <directory> <text to search> <replacement text> [logging file].
 * The [log file] parameter is optional, if you want to save a log in a txt file.
 * Example of an execution line:
 * groovy SearchAndReplace.groovy C:\Users\josuc\Desktop\PruebaTecnicaAccenture "Hello World" "Hello world from new pattern" log.txt
 * 
 * @author Josue
 */
class SearchAndReplace {
    static void main(String[] args) {
        // Validation that the command contains the parameters required by the algorithm to be executed.
        if (args.length < 3 || args.length > 4) {
            println("Use: groovy SearchAndReplace.groovy <directory> <text to search> <replacement text> [logging file]")
            System.exit(1)
        }

        // The variables are declared by taking the arguments.
        def directory = args[0] 
        def serchPattern = args[1]
        def raplacePattern = args[2]
        def loggingFile = args.length == 4 ? args[3] : null
        def files = searchFiles(directory) // The list of existing files is defined
    
        // Execute the executeSearchAndReplace function.
        executeSearchAndReplace(directory, serchPattern, raplacePattern, loggingFile, files)
    }

    /*
    * This method executes the program to search and replace the patterns.
    * @param directory Directory where you want to find the files
    * @param serchPattern Pattern to be replaced
    * @param raplacePattern Substitute pattern
    * @param loggingFile Log file
    * @param files List of all files in the directory
    * @param fileNamesToLog List of all modified files
    */
    static void executeSearchAndReplace(String directory, String serchPattern, String raplacePattern, String loggingFile, List<String> files){
        def start = System.currentTimeMillis()
        def fileNamesToLog = [] 
        // Splits the list of files into two sublists if greater than or equal to 20
        // To work the process in 2 threads
        if (files.size() >= 20){
            def half = files.size() / 2
            def firstHalf = files[0..half - 1]
            def secondHalf = files[half..-1]

            def thread1 = Thread.start {
                firstHalf.each { file ->
                    replaceInFile(file, serchPattern, raplacePattern, loggingFile, fileNamesToLog)
                }
            }

            def thread2 = Thread.start {
                secondHalf.each { file ->
                    replaceInFile(file, serchPattern, raplacePattern, loggingFile, fileNamesToLog)
                }
            }

            // Wait for both threads to finish
            thread1.join()
            thread2.join()

        } else { // In case the number of files is less than 20, only one process is carried out
            files.each { file ->
                replaceInFile(file, serchPattern, raplacePattern, loggingFile, fileNamesToLog)
            }
        }

        if (fileNamesToLog.size() > 0) { // Printout of modified files
            println("${fileNamesToLog.size()} successfully modified files, you can see the complete log at ${loggingFile}")
        } else { 
            println("No matches found in the archives.")
        }

        if (loggingFile) { // Generates the record in case you have specified it in the command
            log(loggingFile, fileNamesToLog)
        }

        def end = System.currentTimeMillis()
        def timeElapsed = end - start
        println("Duration modifying the files: ${timeElapsed} milliseconds") // Printout of run time
    }

    /*
    * This method searches for all the files in the directory in the subdirectories
    * @param directory Directory in which the files are to be found
    * @return List of all existing files
    */
    static List<Path> searchFiles(String directory) {
        def files = []
        def dir = new File(directory)

        dir.eachFileRecurse(FileType.FILES) { file ->
            files.add(file.toPath())
        }
        
        return files
    }

    /*
    * This method replaces the patterns found in a file and makes a record of the modified ones.
    * @param file File where the pattern is to be replaced
    * @param serchPattern Pattern to be replaced
    * @param raplacePattern Substitute pattern
    * @param loggingFile Log file
    * @param fileNamesToLog Logging of modified files
    */
    static void replaceInFile(Path file, String serchPattern, String raplacePattern, String loggingFile, List<String> fileNamesToLog) {
        def content = file.toFile().text
        def lines = content.readLines()
        def modifiedLines = []

        for (int i = 0; i < lines.size(); i++) {
            if (lines[i].contains(serchPattern)) {
                lines[i] = lines[i].replaceAll(serchPattern, raplacePattern)
                modifiedLines << (i + 1)  // Stores the modified line number (starting from 1)
            }
        }

        if (!modifiedLines.isEmpty()) {
            fileNamesToLog.add("Modified: ${file.getFileName()}, Modified lines: ${modifiedLines.join(', ')}")
            backupFile(file)
            file.toFile().text = lines.join('\n')
        }
    }

    /*
    * This method makes a backup of a file
    * @param file File to be backed up
    */
    static void backupFile(Path file) {
        def timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
        def fileBackup = file.resolveSibling("${file.getFileName()}.${timestamp}.bak")
        Files.copy(file, fileBackup, StandardCopyOption.REPLACE_EXISTING)
    }

    /*
    * This method generates a txt file with a log of the modified files.
    * @param loggingFile Name of the file to be registered
    * @param fileNamesToLog List of all modified files
    */
    static void log(String loggingFile, List<String> fileNamesToLog) {
        new File(loggingFile).withWriter { writer ->
            fileNamesToLog.each { line ->
                writer.println(line)
            }
        }
    }
}
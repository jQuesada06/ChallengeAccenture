import groovy.io.FileType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date

class SearchAndReplace {
    static void main(String[] args) {
        //Validación de que en el comando vengan los parametros que necesita el algoritmo para ejecutarse
        //Los parametros son <directorio> <texto a buscar> <texto de reemplazo> [archivo de registro] 
        //El parametro [archivo de registro] es opcional, si desea guardar un registro en un txt
        if (args.length < 3 || args.length > 4) {
            println("Uso: groovy SearchAndReplace.groovy <directorio> <texto a buscar> <texto de reemplazo> [archivo de registro *Opcional]")
            System.exit(1)
        }

        //Se deaclaran las variables tomando los argumentos.
        def directory = args[0] 
        def serchPattern = args[1]
        def raplacePattern = args[2]
        def loggingFile = args.length == 4 ? args[3] : null
        def files = searchFiles(directory) //Se define la lista con los archivos que existen
        def registry = []


        def start = System.currentTimeMillis()
        // Divide la lista de archivos en dos sublistas en caso de que sea mayor o igual a 20
        //Para trabajar el proceso en 2 hilos
        if (files.size() >= 20){
            def half = files.size() / 2
            def firstHalf = files[0..half - 1]
            def secondHalf = files[half..-1]

            def thread1 = Thread.start {
                firstHalf.each { file ->
                    replaceInFile(file, serchPattern, raplacePattern, loggingFile, registry)
                }
            }

            def thread2 = Thread.start {
                secondHalf.each { file ->
                    replaceInFile(file, serchPattern, raplacePattern, loggingFile, registry)
                }
            }

            // Espera a que ambos hilos terminen
            thread1.join()
            thread2.join()

        } else { //En caso de que los archivos sean menos de 20 solo se hace un proceso
            files.each { file ->
                replaceInFile(file, serchPattern, raplacePattern, loggingFile, registry)
            }
        }

        if (registry.size() > 0) { //Impresion de los archivos modificados
            println("${registry.size()} archivos modificados con exito, puedes ver el registro completo en ${loggingFile}")
        } else { 
            println("No se encontraron coincidencias en los archivos.")
        }

        if (loggingFile) { //Genera el registro en caso de que lo haya indicado en el comando
            generateRegistry(loggingFile, registry)
        }

        def end = System.currentTimeMillis()
        def timeElapsed = end - start
        println("Duración modificando los archivos: ${timeElapsed} milisegundos") //Impresion del tiempo de ejecucion
    }

    /*
    * Este metodo busca todos los archivos que hay en el directorio en los subdirectorios
    * @param directory Directorio en el que se van a buscar los archivos
    * @return Lista de todos los archivos que existen
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
    * Este metodo reemplaza los patrones encontrados en un archivo y hace un registro de los modificados
    * @param file Archivo donde se va a remplazar el patron
    * @param serchPattern Patron a remplazar
    * @param raplacePattern Patron suplente
    * @param loggingFile Archivo de registro 
    * @param registry Registro de los archivos modificados
    * @return True o False
    */
    static boolean replaceInFile(Path file, String serchPattern, String raplacePattern, String loggingFile, List<String> registry) {
        def content = file.toFile().text
        def lines = content.readLines()
        def modifiedLines = []

        for (int i = 0; i < lines.size(); i++) {
            if (lines[i].contains(serchPattern)) {
                lines[i] = lines[i].replaceAll(serchPattern, raplacePattern)
                modifiedLines << (i + 1)  // Almacena el número de línea modificado (empezando desde 1)
            }
        }

        if (!modifiedLines.isEmpty()) {
            registry.add("Se modificó: ${file.getFileName()}, Líneas modificadas: ${modifiedLines.join(', ')}")
            backupFile(file)
            file.toFile().text = lines.join('\n')
            return true
        }

        return false
    }

    /*
    * Este metodo hace un backup de un archivo
    * @param file Archivo al cual se le va a hacer el backup
    */
    static void backupFile(Path file) {
        def timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
        def fileBackup = file.resolveSibling("${file.getFileName()}.${timestamp}.bak")
        Files.copy(file, fileBackup, StandardCopyOption.REPLACE_EXISTING)
    }

    /*
    * Este metodo genera un archivo txt con un registro de los archivos modificados
    * @param loggingFile Nombre del archivo que se quiere de registro
    * @param registry Lista de todos los archivos modificados
    */
    static void generateRegistry(String loggingFile, List<String> registry) {
        new File(loggingFile).withWriter { writer ->
            registry.each { line ->
                writer.println(line)
            }
        }
    }
}
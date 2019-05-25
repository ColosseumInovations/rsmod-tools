package gg.rsmod.ui.plugin

import gg.rsmod.tools.plugin.PluginPacker
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.streams.toList

/**
 * @author Tom <rspsmods@gmail.com>
 */
class PluginPackerController : Initializable {

    private var icons: List<Image>? = null

    lateinit var primaryStage: Stage

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        loadSettings()

        /**
         * Menu items.
         */
        closeApp.setOnAction {
            Platform.exit()
            System.exit(0)
        }

        openHelp.setOnAction {
            val stage = Stage()
            val loader = FXMLLoader(PluginPackerController::class.java.getResource("/ui/plugin/help.fxml"))
            stage.scene = Scene(loader.load())
            stage.title = "Help"
            stage.initModality(Modality.WINDOW_MODAL)
            stage.initOwner(primaryStage)
            stage.show()
        }

        /**
         * Pack settings.
         */
        compilerButton.setOnAction {
            val chooser = FileChooser()
            chooser.title = "Select Kotlin Compiler (kotlinc) file"

            if (compilerPath.text.isNotBlank()) {
                val oldPath = Paths.get(compilerPath.text)
                chooser.initialDirectory = if (Files.exists(oldPath)) oldPath.parent.toFile() else null
            } else {
                chooser.initialDirectory = Paths.get(".").toFile()
            }

            val file = chooser.showOpenDialog(primaryStage)
            if (file != null) {
                setText(compilerPath, file.absolutePath)
            }
        }

        dependencyButton.setOnAction {
            val chooser = DirectoryChooser()
            chooser.title = "Select dependency folder"

            if (dependencyPath.text.isNotBlank()) {
                val oldPath = Paths.get(dependencyPath.text)
                chooser.initialDirectory = if (Files.exists(oldPath)) oldPath.toFile() else null
            } else {
                chooser.initialDirectory = Paths.get(".").toFile()
            }

            val folder = chooser.showDialog(primaryStage)
            if (folder != null) {
                setText(dependencyPath, folder.absolutePath)
            }
        }

        outputButton.setOnAction {
            val chooser = DirectoryChooser()
            chooser.title = "Select desired output folder"

            if (outputPath.text.isNotBlank()) {
                val oldPath = Paths.get(outputPath.text)
                chooser.initialDirectory = if (Files.exists(oldPath)) oldPath.toFile() else null
            } else {
                chooser.initialDirectory = Paths.get(".").toFile()
            }

            val folder = chooser.showDialog(primaryStage)
            if (folder != null) {
                setText(outputPath, folder.absolutePath)
            }
        }

        sourceButton.setOnAction {
            if (!singleSourceFile.isSelected) {
                var oldDirectory: File? = null
                while (true) {
                    val chooser = DirectoryChooser()
                    chooser.title = "Select path to your plugin source files"

                    if (sourcePath.text.isNotBlank()) {
                        val oldPath = Paths.get(sourcePath.text)
                        chooser.initialDirectory = if (Files.exists(oldPath)) {
                            if (Files.isDirectory(oldPath)) oldPath.toFile() else oldPath.parent.toFile()
                        } else null
                    } else if (oldDirectory != null) {
                        chooser.initialDirectory = oldDirectory
                    } else {
                        chooser.initialDirectory = Paths.get(".").toFile()
                    }

                    val folder = chooser.showDialog(primaryStage)
                    if (folder != null) {
                        if (Files.walk(folder.toPath()).anyMatch { p -> p.fileName.toString().endsWith(".kt") || p.fileName.toString().endsWith(".kts") }) {
                            setText(sourcePath, folder.absolutePath)
                            break
                        } else {
                            oldDirectory = folder
                            alertDialog(Alert.AlertType.ERROR, "Error", "That source directory is not valid!",
                                    "Directory must at least 1 Kotlin file.", primaryStage, icons)
                        }
                    } else {
                        break
                    }
                }
            } else {
                val chooser = FileChooser()
                chooser.title = "Select your plugin source files"

                if (sourcePath.text.isNotBlank()) {
                    val oldPath = Paths.get(sourcePath.text)
                    chooser.initialDirectory = if (Files.exists(oldPath)) {
                        if (Files.isDirectory(oldPath)) oldPath.toFile() else oldPath.parent.toFile()
                    } else null
                } else {
                    chooser.initialDirectory = Paths.get(".").toFile()
                }

                val file = chooser.showOpenDialog(primaryStage)
                if (file != null) {
                    setText(sourcePath, file.absolutePath)
                }
            }
        }

        pluginName.textFormatter = TextFormatter<String> { c ->
            if (c.text == c.controlNewText) {
                return@TextFormatter c
            }
            if (c.controlNewText.isBlank()) {
                c.text = ""
                return@TextFormatter c
            }
            c.text = c.text.replace(" ", "_")
            return@TextFormatter c
        }

        /**
         * Packing plugin.
         */
        jarPlugin.selectedProperty().addListener { _, _, newValue ->
            zipPlugin.isSelected = !newValue

            compilerPath.isDisable = !newValue
            compilerButton.isDisable = !newValue

            dependencyPath.isDisable = !newValue
            dependencyButton.isDisable = !newValue
        }

        zipPlugin.selectedProperty().addListener { _, _, newValue ->
            jarPlugin.isSelected = !newValue

            compilerPath.isDisable = newValue
            compilerButton.isDisable = newValue

            dependencyPath.isDisable = newValue
            dependencyButton.isDisable = newValue
        }


        packPlugin.setOnAction {
            val compiler = Paths.get(compilerPath.text)
            val dependency = Paths.get(dependencyPath.text)
            val sourceFolder = Paths.get(sourcePath.text)
            val outputFolder = Paths.get(outputPath.text)
            val plugin = pluginName.text

            if (jarPlugin.isSelected) {
                if (compilerPath.text.isBlank() || !Files.exists(compiler) || Files.isDirectory(compiler)) {
                    alertDialog(Alert.AlertType.ERROR, "Error", "The Kotlin compiler file does not exist!",
                            "File: ${compilerPath.text}", primaryStage, icons)
                    return@setOnAction
                }

                if (dependencyPath.text.isBlank() || !Files.exists(dependency) || !Files.isDirectory(dependency)) {
                    alertDialog(Alert.AlertType.ERROR, "Error", "The dependency folder does not exist!",
                            "Directory: ${dependencyPath.text}", primaryStage, icons)
                    return@setOnAction
                }
            }

            if (outputPath.text.isBlank() || !Files.exists(outputFolder) || !Files.isDirectory(outputFolder)) {
                alertDialog(Alert.AlertType.ERROR, "Error", "The output folder does not exist!",
                        "Directory: ${outputPath.text}", primaryStage, icons)
                return@setOnAction
            }

            if (sourcePath.text.isBlank() || !Files.exists(sourceFolder) || !Files.isDirectory(sourceFolder) && !sourceFolder.fileName.toString().endsWith(".kt") && !sourceFolder.fileName.toString().endsWith(".kts")) {
                alertDialog(Alert.AlertType.ERROR, "Error", "The plugin source folder does not exist!",
                        "Directory: ${sourcePath.text}", primaryStage, icons)
                return@setOnAction
            }

            if (plugin.isBlank()) {
                alertDialog(Alert.AlertType.ERROR, "Error", null,
                        "You have not set a plugin name.", primaryStage, icons)
                return@setOnAction
            }

            packPlugin.isDisable = true
            if (jarPlugin.isSelected) {
                val dependencies = Files.walk(dependency).filter { it.fileName.toString().endsWith(".jar") }.toList()
                if (PluginPacker().compileBinary(compilerPath = compiler.toAbsolutePath().toString(),
                                dependencies = dependencies,
                                pluginName = plugin,
                                outputPath = outputFolder,
                                paths = Files.walk(sourceFolder).toList())) {
                    saveSettings()
                    alertDialog(Alert.AlertType.INFORMATION, "Success!", "Your plugin was packed!",
                            "Your plugin was packed to: ${outputFolder.toAbsolutePath().toString() + "\\" + plugin + ".jar"}", primaryStage, icons)
                } else {
                    alertDialog(Alert.AlertType.ERROR, "Error", "Could not compile plugins!",
                            "Make sure your Kotlin compiler is working properly and files do not contain errors!", primaryStage, icons)
                }
            } else if (zipPlugin.isSelected) {
                val prePath = Paths.get(".").toAbsolutePath().toString()
                if (PluginPacker().compileSource(pluginName = plugin, outputPath = outputFolder, paths = Files.walk(sourceFolder).toList(),
                                removeParent = prePath.substring(0 until prePath.length - 1).replace("\\", "/"))) {
                    saveSettings()
                    alertDialog(Alert.AlertType.INFORMATION, "Success!", "Your plugin was packed!",
                            "Your plugin was packed to: ${outputFolder.toAbsolutePath().toString() + "\\" + plugin + ".zip"}", primaryStage, icons)
                } else {
                    alertDialog(Alert.AlertType.ERROR, "Error", "Something went wrong!",
                            "Make sure you have efficient privilege to add files to: $outputFolder", primaryStage, icons)
                }
            }
            packPlugin.isDisable = false
        }
    }

    private fun setText(field: TextField, text: String) {
        field.text = text
        field.positionCaret(field.text.length - 1)
    }

    private fun loadSettings() {
        try {
            val file = Paths.get(System.getProperty("user.home"), PROPERTY_FILE)

            if (Files.exists(file)) {
                val settings = Properties()
                Files.newBufferedReader(file).use { reader ->
                    settings.load(reader)

                    compilerPath.text = settings.getProperty("compilerPath", "")
                    dependencyPath.text = settings.getProperty("dependency", "")
                    outputPath.text = settings.getProperty("outputPath", "")
                    sourcePath.text = settings.getProperty("pluginFilesPath", "")
                    pluginName.text = settings.getProperty("plugin", "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val source = Paths.get(sourcePath.text)
        if (Files.exists(source)) {
            singleSourceFile.isSelected = !Files.isDirectory(source)
        }
    }

    private fun saveSettings() {
        try {
            val properties = Properties()
            properties.setProperty("compilerPath", compilerPath.text)
            properties.setProperty("dependency", dependencyPath.text)
            properties.setProperty("outputPath", outputPath.text)
            properties.setProperty("pluginFilesPath", sourcePath.text)
            properties.setProperty("plugin", pluginName.text)

            val file = Paths.get(System.getProperty("user.home"), PROPERTY_FILE)
            Files.newBufferedWriter(file).use { writer ->
                properties.store(writer, "RS Mod Plugin Packer Properties")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun alertDialog(type: Alert.AlertType, title: String, header: String?, context: String, parent: Stage?, icon: List<Image>?) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = header
        alert.contentText = context
        if (parent != null) {
            alert.initModality(Modality.APPLICATION_MODAL)
            alert.initOwner(parent)
        }
        if (icon != null) {
            val stage = alert.dialogPane.scene.window as Stage
            stage.icons.addAll(icon)
        }
        alert.showAndWait()
    }

    @FXML
    private lateinit var closeApp: MenuItem

    @FXML
    private lateinit var openHelp: MenuItem

    @FXML
    private lateinit var compilerPath: TextField

    @FXML
    private lateinit var compilerButton: Button

    @FXML
    private lateinit var dependencyPath: TextField

    @FXML
    private lateinit var dependencyButton: Button

    @FXML
    private lateinit var singleSourceFile: CheckBox

    @FXML
    private lateinit var sourcePath: TextField

    @FXML
    private lateinit var sourceButton: Button

    @FXML
    private lateinit var outputPath: TextField

    @FXML
    private lateinit var outputButton: Button

    @FXML
    private lateinit var pluginName: TextField

    @FXML
    private lateinit var jarPlugin: CheckBox

    @FXML
    private lateinit var zipPlugin: CheckBox

    @FXML
    private lateinit var packPlugin: Button

    companion object {
        private const val PROPERTY_FILE = "rsmod-packer.properties"
    }
}

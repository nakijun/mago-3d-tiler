package command;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class Command {
    private CommandOption commandOption;
    private String version = "0.0.1";
    private boolean isHelp = false;
    private boolean isVersion = false;

    public Command() {
        commandOption = new CommandOption();
        commandOption.setInputType(CommandOption.InputType.IN_3DS);
        commandOption.setOutputType(CommandOption.OutputType.OUT_GLTF);
        commandOption.setRecursive(false);
        commandOption.setSwapYZ(false);
    }

    public void startCommand(String[] args) {
        log.info("how");
        for (String arg : args) {
            if (arg.contains("-version") || arg.contains("-v")) {
                isVersion = true;
            } else if (arg.contains("-help") || arg.contains("--help") || arg.contains("-h")) {
                isHelp = true;
            } else if (arg.contains("-quiet")) {
                commandOption.setQuiet(true);
            } else if (arg.contains("-input=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputPath(new File(path).toPath());
            } else if (arg.contains("-output=")) {
                String path = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputPath(new File(path).toPath());
            } else if (arg.contains("-inputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setInputType(CommandOption.InputType.fromExtension(value));
            } else if (arg.contains("-outputType=")) {
                String value = arg.substring(arg.indexOf("=") + 1);
                commandOption.setOutputType(CommandOption.OutputType.fromExtension(value));
            } else if (arg.contains("-recursive") || arg.contains("--R")) {
                commandOption.setRecursive(true);
            }
        }

        printLogo();
        if (isHelp) {
            printHelp();
            return;
        } else if (isVersion) {
            printVersion();
            return;
        } else if (commandOption.getInputPath() == null) {
            errlog("inputPath is not defined.");
            return;
        }  else if (commandOption.getOutputPath() == null) {
            errlog("outputPath is not defined.");
            return;
        }
//        logWithoutTime("inputPath : " + commandOption.getInputPath().toAbsolutePath().toString());
//        logWithoutTime("outputPath : " + commandOption.getOutputPath().toAbsolutePath().toString());
//        logWithoutTime("inputType : " + commandOption.getInputType().getExtension());
//        logWithoutTime("outputType : " + commandOption.getOutputType().getExtension());

        File inputPathFile = commandOption.getInputPath().toFile();
        File outputPathFile = commandOption.getOutputPath().toFile();
        excute(commandOption, inputPathFile, outputPathFile);
        printEnd();
    }

    private void excute(CommandOption commandOption, File inputPath, File outputPath) {
        String inputExtension = commandOption.getInputType().getExtension();
        String outputExtension = commandOption.getOutputType().getExtension();
        if (inputPath.isFile() && (FileUtils.getExtension(inputPath.getName()).equals(inputExtension))) {
            String outputFile = FileUtils.changeExtension(inputPath.getName(), outputExtension);
            File output = new File(commandOption.getOutputPath().toAbsolutePath().toString() + File.separator + outputFile);
            log("convert : " + inputPath.getAbsolutePath() + " -> " + output.getAbsolutePath());
            GaiaScene scene = DataLoader.load(inputPath.getAbsolutePath(), null);
            if (commandOption.getOutputType() == CommandOption.OutputType.OUT_GLB) {
                GltfWriter.writeGlb(scene, output.getAbsolutePath());
            } else if (commandOption.getOutputType() == CommandOption.OutputType.OUT_GLTF) {
                GltfWriter.writeGltf(scene, output.getAbsolutePath());
            }
        } else if (inputPath.isDirectory()) {
            for (File child : inputPath.listFiles()) {
                excute(commandOption, child, outputPath);
            }
        } else {
            //log("skip : " + child.getName());
        }
    }

    private void printLogo() {
        if (commandOption.isQuiet()) {
            return;
        }
        System.out.println(
                " _______  ___      _______  _______  __   __  _______ \n" +
                "|       ||   |    |   _   ||       ||  |_|  ||   _   |\n" +
                "|    _  ||   |    |  |_|  ||  _____||       ||  |_|  |\n" +
                "|   |_| ||   |    |       || |_____ |       ||       |\n" +
                "|    ___||   |___ |       ||_____  ||       ||       |\n" +
                "|   |    |       ||   _   | _____| || ||_|| ||   _   |\n" +
                "|___|    |_______||__| |__||_______||_|   |_||__| |__|\n" +
                "==================[Plasma Converter]==================");
    }

    private void printEnd() {
        if (commandOption.isQuiet()) {
            return;
        }
        System.out.println("=============[END][Plasma Gltf Converter]=============");
    }

    private void printVersion() {
        logWithoutTime("==========================[HELP][Version]==========================");
        logWithoutTime("Author: Gaia3D-znkim");
        logWithoutTime("Version: " + version);
        logWithoutTime("==========================[HELP][Version]==========================");
    }

    private void printHelp() {
        logWithoutTime("==========================[HELP][How to use]==========================");
        logWithoutTime("Usage: java -jar tiler.jar -input=<inputPath> -output=<outputPath> -inputType=<inputType> -outputType=<outputType> -recursive");
        logWithoutTime("Example: java -jar tiler.jar -input=C:\\sample\\sampleFile.3ds -output=C:\\sample\\sampleFile.gltf");
        logWithoutTime("Example: java -jar tiler.jar -input=C:\\samplePath\\ -output=C:\\samplePath\\ -inputType=3ds -outputType=gltf");
        logWithoutTime("input : input file path or directory path");
        logWithoutTime("output : output file path or directory path");
        logWithoutTime("inputType : input file type");
        logWithoutTime("outputType : output file type");
        logWithoutTime("recursive : recursive convert");
        logWithoutTime("help : print help");
        logWithoutTime("version : print version");
        logWithoutTime("==========================[HELP][How to use]==========================");
        logWithoutTime("inputType : 3ds, obj, fbx, dae, gltf, glb");
        logWithoutTime("outputType : gltf, glb");
        logWithoutTime("DefaultValue: inputType = 3ds, outputType = gltf");
        logWithoutTime("==========================[HELP][How to use]==========================");
    }

    private void logWithoutTime(String message) {
        if (commandOption.isQuiet()) {
            return;
        }
        System.out.println("[P] " + message);
    }

    private void log(String message) {
        if (commandOption.isQuiet()) {
            return;
        }
        String nowDate = getStringDate();
        System.out.println("[P]["+ nowDate +"] " + message);
    }

    private void errlog(String message) {
        if (commandOption.isQuiet()) {
            return;
        }
        String nowDate = getStringDate();
        System.out.println("[P]["+ nowDate +"] " + message);
    }

    private String getStringDate() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(date);
    }
}

package work.ready.examples.database;

import work.ready.core.database.DatabaseManager;
import work.ready.core.database.generator.*;
import work.ready.core.tools.PathUtil;

public class CodeGenerator {

    public static void main(String[] args) {
        customizedWithPath();
    }

    public static void easy() {
        String modelPackage = "work.ready.test.model";
        String servicePackage = "work.ready.test.service";
        var generator = new Generator(DatabaseManager.MAIN_CONFIG_NAME, modelPackage, servicePackage);
        generator.generate();
    }

    public static void customized() {
        String modelPackage = "work.ready.test.model";
        String baseModelPackage = modelPackage + ".base";
        String servicePackage = "work.ready.test.service";

        var generator = new Generator(Generator.getMainDatasource(), Generator.getMainMetaBuilder(),
                new BaseModelGenerator(baseModelPackage),
                new ModelGenerator(modelPackage, baseModelPackage),
                new ServiceInterfaceGenerator(servicePackage, modelPackage),
                new ServiceImplGenerator(servicePackage, modelPackage)
        );
        generator.generate();
    }

    public static void customizedWithPath() {
        String modelPackage = "work.ready.test.model";
        String baseModelPackage = modelPackage + ".base";
        String servicePackage = "work.ready.test.service";

        String path = PathUtil.getProjectRootPath() + "/src/test/java/";
        String baseModelPath = path + baseModelPackage.replace(".", "/");
        String modelPath = path + modelPackage.replace(".", "/");
        String servicePath = path + servicePackage.replace(".", "/");
        String serviceImplPath = path + (servicePackage + ".impl").replace(".", "/");

        String ds = "main";
        var generator = new Generator(Generator.getDatasource(ds), Generator.getMetaBuilder(ds),
                new BaseModelGenerator(baseModelPackage, baseModelPath),
                new ModelGenerator(modelPackage, baseModelPackage, modelPath),
                new ServiceInterfaceGenerator(servicePackage, modelPackage, servicePath),
                new ServiceImplGenerator(servicePackage, modelPackage, serviceImplPath)
        );
        generator.setGenerateDataDictionary(true);
        generator.setGenerateRemarks(true);
        generator.generate();
    }

}

package work.ready.examples.ioc_aop.component;

import work.ready.core.component.proxy.CodeGenerator;

import java.lang.reflect.Method;
import java.util.Map;

public class CodeWorker implements CodeGenerator {

    private boolean isEnable = false;
    private boolean isReplace = false;
    private String methodName;

    @Override
    public CodeGenerator generatorCode(Class<?> target, Method method, Map<String, Object> clazzData, Map<String, Object> methodData) {
        CodeInjector injector = method.getAnnotation(CodeInjector.class);
        var worker = new CodeWorker();
        worker.methodName = method.getName();
        worker.isReplace = injector.replace();
        worker.isEnable = injector.enable();
        return worker;
    }

    @Override
    public boolean isReplace() {
        return isReplace;
    }

    @Override
    public String getInsertCode() {
        return isEnable ? "System.err.println(\" before " + methodName + " ==> This is the java code INSERTED here.\");" : null;
    }

    @Override
    public String getReplaceCode() {
        return isEnable ? "System.err.println(\" " + methodName + " ==> The original method has been REPLACED by the java code here.\"); returnObject = \"java code replaced\"; " : null;
    }

    @Override
    public String getAppendCode() {
        return isEnable ? "System.err.println(\" after " + methodName + " ==> This is the java code APPENDED here.\");" : null;
    }

}

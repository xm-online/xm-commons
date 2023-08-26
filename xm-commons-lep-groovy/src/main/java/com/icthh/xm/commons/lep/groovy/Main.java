package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Main {

    public static String LEP_PROTOCOL = "lep://";

    static String aGroovy = "" +
            "package TENANT.entity.lep.commons.a\n" +
            "\n" +
            "import TENANT.entity.lep.commons.b.B\n" +
            "import static TENANT.entity.lep.commons.b.B.hello\n" +
            "import static TENANT.entity.lep.commons.Int.F\n" +
            "import static TENANT.entity.lep.commons.b.B.C.TEST\n" +
            "import static TENANT.entity.lep.commons.b.B.TEST_B\n" +
            "import static TENANT.entity.lep.commons.b.B.C.D.En.VAR1\n" +
            "\n" +
            "\n" +
            "class A {\n" +
            "    B b;\n" +
            "    def initFields() {\n" +
            "        b = new B('B');\n" +
            "    }\n" +
            "    public A(){println('call A()')}\n" +
            "    public A(String input) {" +
            "        println('call A(input)')\n" +
            "        println(TEST_B)\n" +
            "        println(F)\n" +
            "        hello()\n" +
            "        println(TEST)\n" +
            "        println(VAR1.name())\n" +
            "}\n" +
            "    public static void main(String [] args) {\n" +
            "        def b = new A('a').initFields(); b.initFields()\n" +
            "    }\n" +
            "}";

    static String bGroovy = "" +
            "package TENANT.entity.lep.commons.b\n" +
            "\n" +
            "import TENANT.entity.lep.commons.a.A\n" +
            "\n" +
            "class B {\n" +
            "    public static void hello(){println('hello')}\n" +
            "    public static final String TEST_B = 'test_b'\n" +
            "    public static class C {\n" +
            "        public static final String TEST = 'test'\n" +
            "        public static class D {" +
            "           enum En {" +
            "               VAR1, VAR2;" +
            "           }" +
            "        }\n" +
            "    }\n" +
            "    public static class E {}\n" +
            "    public B(String input){}\n" +
            "\n" +
            "    A a;\n" +
            "    def initFields() {\n" +
            "        a = new A('a');\n" +
            "    }\n" +
            "\n" +
            "}";

    static String intGroovy = "" +
        "package TENANT.entity.lep.commons \n" +
        "    public interface Int {\n" +
        "        String F = \"fff\";\n" +
        "    }\n";

    static String myCommonsGroovy = "" +
        "\n" +
        "import TENANT.entity.lep.commons.a.A\n" +
        "import TENANT.entity.lep.commons.b.B\n" +
        "println('RUN COMMONS GROOVY')\n" +
        "return [a:new A('a'), b:new B('b')]\n";


    private static final Map<String, XmLepConfigFile> scripts = new HashMap<String, XmLepConfigFile>() {{
        put("TENANT/entity/lep/commons/a/A$$tenant",  new XmLepConfigFile("TENANT/entity/lep/commons/a/A$$tenant", aGroovy));
        put("TENANT/entity/lep/commons/b/B", new XmLepConfigFile("TENANT/entity/lep/commons/b/B", bGroovy));
        put("TENANT/entity/lep/commons/Int", new XmLepConfigFile("TENANT/entity/lep/commons/Int", intGroovy));
        put("commons/lep/myCommons", new XmLepConfigFile("commons/myCommons", myCommonsGroovy));
    }};

    public static void main(String[] args) throws IOException, ResourceException, ScriptException {
        try {
            GroovyScriptEngine gse = new GroovyScriptEngine(new LepResourceConnector("TENANT", "entity", new TenantAliasService(), scripts), new GroovyClassLoader());

            System.out.println("=");

            // if class definitions in file == 1 that we do parse class else createScript
            gse.getGroovyClassLoader().parseClass(aGroovy, "TENANT/entity/lep/commons/a/A");

            System.out.println("==");

            gse.getGroovyClassLoader().parseClass(myCommonsGroovy, "commons/environment/myCommons");

            System.out.println("===");
            //gse.createScript("commons/environment/myCommons.groovy", new Binding());

            System.out.println("===ABs");
            //gse.createScript("TENANT/entity/lep/commons/a/AB$$tenant.groovy", new Binding());
            System.out.println("===ABe");

            System.out.println("=====");
            var result = gse.run("commons/environment/myCommons", new Binding());
            System.out.println(result);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main2(String[] args) {
        System.out.println(bGroovy);
    }
}

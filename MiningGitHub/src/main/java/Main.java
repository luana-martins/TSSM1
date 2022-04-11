import br.ufba.jnose.core.Config;
import br.ufba.jnose.core.JNoseCore;
import br.ufba.jnose.dto.TestClass;
import br.ufba.jnose.dto.TestSmell;
import com.github.mauricioaniche.ck.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    private static String ghKey = "ghKey here";

    private static int sleepStep = 3001; //3 segundo para cada projeto

    private static int startNumberList = 1;

    private static int endNumberList = 147992;

    private static String projectList = "project_list.txt";

    private static List<String> listaProcessados;

    private static BufferedWriter writerProjetos;

    private static ResultWriter writerCK;

    private static String pastaResultado = "resultados";

    private static CSVPrinter csv;

    private static int contLinhaAtual;

    public static void main(String[] args) {

        try {

            File file = new File("projetos.csv");

            boolean fileExiste = file.exists();

            if(fileExiste == false)file.createNewFile();

            writerProjetos = new BufferedWriter(new FileWriter(file,true));

            if(fileExiste == false) {
                csv = new CSVPrinter(writerProjetos, CSVFormat.DEFAULT
                        .withHeader(
                                "Repo_id", "Full_name", "owner_id", "updated_at",
                                "size", "stargazers", "Subscribers", "forks_count",
                                "open_issues", "watchers", "has_downloads", "has_issues",
                                "has_pages", "has_wiki", "has_projects", "git_url",
                                "clone_url", "last_commit_sha",
                                "testsmells_file", "CK_File_Class", "CK_File_Method"));
            }else{
                csv = new CSVPrinter(writerProjetos, CSVFormat.DEFAULT);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(pastaResultado);
        if(!file.exists()){
            file.mkdir();
        }

        listaProcessados = new ArrayList<>();

        carregarProjetosProcessados();

        contLinhaAtual = 1;
        // Ler o arquivo linha por linha "project_list.txt"
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(projectList));
            String line = reader.readLine();
            while (line != null) {
                try {
                    if (contLinhaAtual >= startNumberList && contLinhaAtual <= endNumberList) {
                        Boolean jaProcessado = listaProcessados.contains(line);
                        if (!jaProcessado) {
                            processarProjeto(line);
                        } else {
                            System.out.println(contLinhaAtual + " já processado -> " + line);
                        }
                    }
                    line = reader.readLine();
                    contLinhaAtual++;
                }catch (Exception e){
                    salvarFile(line, "error.txt");
                }catch (Error e){
                    salvarFile(line, "error.txt");
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void processarProjeto(String nomeProjeto){
        Date startDate = new Date();
        System.out.println("Processando: " + nomeProjeto);

        try {

            GitHub github = null;

            if(ghKey.isEmpty() == false) {
                github = GitHub.connectUsingOAuth(ghKey);
            }else{
                github = GitHub.connectAnonymously();
            }

            GHRepository repository;

            try {
                repository = github.getRepository(nomeProjeto);

                if (repository.isFork() == false) {

                    salvarProjetoPoocessado(repository.getFullName());

                    if (repository.getLanguage() != null && repository.getLanguage().equalsIgnoreCase("java")) {

                        System.out.println(contLinhaAtual + " ok -> " + repository.getFullName());
                        //1 - clonar projeto
                        File diretorio = clonar(repository);
                        if (diretorio.exists()) System.out.println("Clonado: " + repository.getFullName());

                        //2 - processar JNose
                        String pathTestSmellsFile = processarJNose(repository, diretorio);

                        //3 - processar CK
                        String[] filesCK = null;
                        if (pathTestSmellsFile != null) {
                            filesCK = processarCK(repository, diretorio);
                        }

                        if (filesCK != null && filesCK.length == 2) {
                            salvarResultadoFile(repository, pathTestSmellsFile, filesCK[0], filesCK[1]);
                        }

                        //3 - apagar projeto
                        apagarProjeto(diretorio);
                    } else {
                        System.out.println(contLinhaAtual + " não é java -> " + repository.getFullName());
                        salvarFile(repository.getFullName(), "naoEhJava.txt");
                    }
                } else {
                    System.out.println(contLinhaAtual + " é fork -> " + repository.getFullName());
                    salvarFile(repository.getFullName(), "ehFork.txt");
                }
            }catch (GHFileNotFoundException e){
                System.out.println(contLinhaAtual + " não existe mais -> " + nomeProjeto);
                salvarFile(nomeProjeto, "naoExiste.txt");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Date endDate = new Date();

        long tempoProcessado = endDate.getTime() - startDate.getTime();

        try {
            if(tempoProcessado < sleepStep) {
                Thread.sleep(sleepStep);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String processarJNose(GHRepository repository, File directory){
        System.out.println("Processando JNose: " + directory.getName());

        String pathTestSmellsFile = null;

        Config conf = new Config() {
            public Boolean assertionRoulette() {
                return true;
            }
            public Boolean conditionalTestLogic() {
                return true;
            }
            public Boolean constructorInitialization() {
                return true;
            }
            public Boolean defaultTest() {
                return true;
            }
            public Boolean dependentTest() {
                return true;
            }
            public Boolean duplicateAssert() {
                return true;
            }
            public Boolean eagerTest() {
                return true;
            }
            public Boolean emptyTest() {
                return true;
            }
            public Boolean exceptionCatchingThrowing() {
                return true;
            }
            public Boolean generalFixture() {
                return true;
            }
            public Boolean mysteryGuest() {
                return true;
            }
            public Boolean printStatement() {
                return true;
            }
            public Boolean redundantAssertion() {
                return true;
            }
            public Boolean sensitiveEquality() {
                return true;
            }
            public Boolean verboseTest() {
                return true;
            }
            public Boolean sleepyTest() {
                return true;
            }
            public Boolean lazyTest() {
                return true;
            }
            public Boolean unknownTest() {
                return true;
            }
            public Boolean ignoredTest() {
                return true;
            }
            public Boolean resourceOptimism() {
                return true;
            }
            public Boolean magicNumberTest() {
                return true;
            }
            public Integer maxStatements() {
                return 30;
            }
        };

        JNoseCore jNoseCore = new JNoseCore(conf);
        List<TestClass> lista = null;
        try {
            lista = jNoseCore.getFilesTest(directory.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(lista.size() > 0){
            System.out.println("Foram encontrados " + lista.size() + " classes de teste.");
        }else{
            System.out.println("O Projeto não tem testes: " + directory.getName());
            salvarFile(repository.getFullName(), "naoTemTestes.txt");
        }

        for(TestClass testClass : lista){
            System.out.println(testClass.getPathFile() + " - " + testClass.getProductionFile() + " - " + testClass.getJunitVersion());

            if(testClass.getListTestSmell().size() > 0){
                System.out.println("Foram encontrados " + testClass.getListTestSmell().size() + " TestsSmells.");
                pathTestSmellsFile = salvarTestSmellsFile(repository,testClass);
            }else{
                System.out.println("Não foi encontrados TestSmells no Projeto: " + directory.getName());
            }

//            System.out.println(testClass.getLineSumTestSmells());
        }

        return pathTestSmellsFile;
    }

    private static void apagarProjeto(File directory){
        System.out.println("Apagando Projeto: " + directory.getName());
        if(directory.exists()){
            try {
                FileUtils.deleteDirectory(directory);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private static File clonar(GHRepository repository){
        System.out.println("Clonando Projeto: " + repository.getName());

        String diretorioName = repository.getName();
        File directory = new File(diretorioName);

        if(directory.exists()){
            try {
                FileUtils.deleteDirectory(directory);
            }catch (Exception e){
                e.printStackTrace();
            }
            directory.mkdir();
        }else{
            directory.mkdir();
        }

        try {
            Git.cloneRepository().setURI(repository.getHttpTransportUrl())
                    .setBranch(repository.getDefaultBranch())
                    .setDirectory(directory)
                    .call();
        } catch (GitAPIException e) {
            System.out.println("Erro: " + repository.getFullName());
            e.printStackTrace();
        }

        return directory;
    }

    private static String[] processarCK(GHRepository repository, File pathFile){
        System.out.println("Processar CK: " + repository.getName());

        String[] files = new String[2];

        String path = pathFile.getAbsolutePath();
        Boolean useJars = false;
        int maxAtOnce = 0;
        Boolean variablesAndFields = false;

        String nomeProjeto = repository.getOwnerName() + "_" + repository.getName();

        files[0] = pastaResultado+File.separator+nomeProjeto+"_class.csv";
        files[1] = pastaResultado+File.separator+nomeProjeto+"_method.csv";

        try {
            writerCK = new ResultWriter(files[0], files[1], pastaResultado+File.separator+nomeProjeto+"_variable.csv", pastaResultado+File.separator+nomeProjeto+"_field.csv", variablesAndFields);
        } catch (IOException e) {
            e.printStackTrace();
        }
        (new CK(useJars, maxAtOnce, variablesAndFields)).calculate(path, new CKNotifier() {
            public void notify(CKClassResult result) {
                try {
                    writerCK.printResult(result);
                } catch (IOException var3) {
                    throw new RuntimeException(var3);
                }
            }

            public void notifyError(String sourceFilePath, Exception e) {
                System.err.println("Error in " + sourceFilePath);
                e.printStackTrace(System.err);
            }
        });
        try {
            writerCK.flushAndClose();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    private static String salvarTestSmellsFile(GHRepository repository, TestClass testClass){

        List<TestSmell> listaTestSmells = testClass.getListTestSmell();

        String fileName = pastaResultado+File.separator+repository.getOwnerName()+"_"+repository.getName()+"_testsmells.csv";

        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName));
            CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader(
                            "projectName","className", "classPathFile", "productionFile",
                            "numberMethods", "numberLine", "junitVersion", "sumtestSmells",
                            "name", "method", "range"
                    ));

            for(TestSmell testSmell : listaTestSmells){
                csv.printRecord(
                        testClass.getProjectName(), testClass.getName(),testClass.getPathFile(),testClass.getProductionFile(),
                        testClass.getNumberMethods(),testClass.getNumberLine(),testClass.getJunitVersion(),testClass.getJunitVersion(),
                        testSmell.getName(), testSmell.getMethod(), testSmell.getRange()
                );
            }
            csv.flush();
        }catch (Exception e){
            e.printStackTrace();
        }

        return fileName;
    }

    private static void salvarResultadoFile(GHRepository repo, String testSmellsFile, String ckFileClass, String ckFileMethods){

        try {
            csv.printRecord(
                    repo.getId(), repo.getFullName(), repo.getOwner().getId(), repo.getUpdatedAt(),
                    repo.getSize(), repo.getStargazersCount(), repo.getSubscribersCount(), repo.getForksCount(),
                    repo.getOpenIssueCount(), repo.getWatchersCount(), repo.hasDownloads(), repo.hasIssues(),
                    repo.hasPages(), repo.hasWiki(), repo.hasProjects(), repo.getGitTransportUrl(),
                    repo.getHttpTransportUrl(), repo.getBranch(repo.getDefaultBranch()).getSHA1(),
                    testSmellsFile,ckFileClass,ckFileMethods
            );

            csv.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static void salvarProjetoPoocessado(String projetoProcessado){

        listaProcessados.add(projetoProcessado);

        try {
            FileWriter myWriter = new FileWriter("listaProcessados.txt",true);
            myWriter.write(projetoProcessado+"\n");
            myWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void carregarProjetosProcessados(){

        File file = new File("listaProcessados.txt");

        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(
                    "listaProcessados.txt"));
            String line = reader.readLine();
            while (line != null) {
                listaProcessados.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void salvarFile(String projeto, String listaFile){
        try {
            FileWriter myWriter = new FileWriter(listaFile,true);
            myWriter.write(projeto +"\n");
            myWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


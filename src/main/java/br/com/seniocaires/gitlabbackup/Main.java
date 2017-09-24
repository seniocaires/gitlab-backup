package br.com.seniocaires.gitlabbackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Main {

  private static WebClient webClientPrincipal = new WebClient(BrowserVersion.CHROME);

  private static List<String> linkProjetos = new ArrayList<String>();

  private static List<String> linkGrupos = new ArrayList<String>();

  private static final String URL_GITLAB = System.getenv("URL_GITLAB");

  private static final String USUARIO = System.getenv("USUARIO");

  private static final String SENHA = System.getenv("SENHA");
  
  private static final String BACKUP_PATH = System.getenv("BACKUP_PATH");

  public static void main(String[] args) {

    init();

    logar();

    buscarLinkGrupos();
    buscarLinkProjetos();

    clonar();

    System.exit(0);
  }

  @SuppressWarnings("deprecation")
  private static void clonar() {

    Repository repository = null;
    Git git = null;
    File path = null;
    FileRepositoryBuilder builder = null;
    PullCommand pull = null;
    for (String linkProjeto : linkProjetos) {
      mensagem("Clone/Pull " + linkProjeto);
      path = new File(BACKUP_PATH + File.separator + linkProjeto.replace(".git", ""));
      try {
        if (path.exists()) {
          builder = new FileRepositoryBuilder();
          repository = builder.setWorkTree(path).readEnvironment().build();
          git = new Git(repository);
          pull = git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(USUARIO, SENHA));

          pull.setTimeout(6000);
          pull.setProgressMonitor(new TextProgressMonitor());
          PullResult pullResult = pull.call();
          if (pullResult != null) {
            mensagem("Pulling " + linkProjeto);
          } else {
            mensagem("PullResult null " + linkProjeto);
          }
          if (pullResult.getRebaseResult() != null) {
            mensagem(String.valueOf(pullResult.getRebaseResult().getStatus()));
          }
          if (pullResult.getMergeResult() != null) {
            mensagem(String.valueOf(pullResult.getMergeResult().getMergeStatus()));
          }
          mensagem(pullResult.getFetchResult().getMessages());
          for (final TrackingRefUpdate u : pullResult.getFetchResult().getTrackingRefUpdates()) {
            mensagem("local " + u.getLocalName() + " remote " + u.getRemoteName() + " result " + u.getResult() + " toString " + u.toString());
          }

          for (Ref f : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
            mensagem("checked out branch " + f.getName() + ". HEAD: " + git.getRepository().getRef(Constants.HEAD));
          }
        } else {
          git = Git.cloneRepository().setTimeout(6000).setURI(URL_GITLAB + "/" + linkProjeto).setCredentialsProvider(new UsernamePasswordCredentialsProvider(USUARIO, SENHA)).setProgressMonitor(new TextProgressMonitor()).setCloneAllBranches(true).setDirectory(path).call();
          for (Ref f : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
            mensagem("checked out branch " + f.getName() + ". HEAD: " + git.getRepository().getRef(Constants.HEAD));
          }
        }
      } catch (RefNotAdvertisedException e) {
        mensagem("Erro ao clonar/pull " + linkProjeto + " " + e);
      } catch (IOException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (WrongRepositoryStateException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (InvalidConfigurationException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (DetachedHeadException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (InvalidRemoteException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (CanceledException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (RefNotFoundException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (NoHeadException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (TransportException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } catch (GitAPIException e) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
      } finally {
        if (git != null) {
          git.close();
        }
        if (repository != null) {
          repository.close();
        }
        repository = null;
        git = null;
        path = null;
        builder = null;
        pull = null;
      }
    }
  }

  private static void buscarLinkProjetos() {

    HtmlPage pagina;
    int numeroPagina;
    String url;
    try {

      for (String linkGrupo : linkGrupos) {

        numeroPagina = 1;
        url = URL_GITLAB + linkGrupo;
        do {

          mensagem("Acessando página do grupo " + linkGrupo);
          pagina = webClientPrincipal.getPage(url);

          for (Object linkProjeto : pagina.getByXPath("//span[@class='monospace']")) {
            mensagem("Adicionando " + ((HtmlElement) linkProjeto).getTextContent());
            linkProjetos.add(((HtmlElement) linkProjeto).getTextContent());
          }

          numeroPagina++;
          url = URL_GITLAB + linkGrupo + "?projects_page=" + numeroPagina;

        } while (existeProximaPagina(pagina, linkGrupo + "?projects_page=" + numeroPagina));
      }
    } catch (FailingHttpStatusCodeException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (MalformedURLException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (SecurityException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private static boolean existeProximaPagina(HtmlPage pagina, String proximoLink) {
    List<Object> nexts = pagina.getByXPath("//li[@class='next']");
    if (nexts.isEmpty()) {
      return false;
    } else if (("<li class=\"next\">  <a rel=\"next\" href=\"" + proximoLink + "\">    Next  </a></li>").equals(((HtmlElement) nexts.get(0)).asXml().replaceAll("\n", "").replaceAll("\r", ""))) {
      return true;
    } else {
      return false;
    }
  }

  private static void buscarLinkGrupos() {

    HtmlPage pagina;
    String url = URL_GITLAB + "/admin/groups";
    int numeroPagina = 1;
    try {
      do {

        mensagem("Acessando página dos grupos " + numeroPagina);
        pagina = webClientPrincipal.getPage(url);

        for (Object linkGrupo : pagina.getByXPath("//a[@class='group-name']")) {
          mensagem("Adicionando " + ((HtmlElement) linkGrupo).getAttribute("href"));
          linkGrupos.add(((HtmlElement) linkGrupo).getAttribute("href"));
        }

        numeroPagina++;
        url = URL_GITLAB + "/admin/groups?page=" + numeroPagina;

      } while (existeProximaPagina(pagina, "/admin/groups?page=" + numeroPagina));
    } catch (FailingHttpStatusCodeException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (MalformedURLException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (SecurityException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private static void init() {
    mensagem("Limpando logs ...");
    new File("log.html").delete();
    webClientPrincipal = new WebClient(BrowserVersion.CHROME);
    mensagem("Criando navegador ...");
    webClientPrincipal.getOptions().setJavaScriptEnabled(false);
    webClientPrincipal.getOptions().setCssEnabled(false);
  }

  private static void logar() {
    try {
      mensagem("Acessando página de login");
      HtmlPage page = webClientPrincipal.getPage(URL_GITLAB + "/users/sign_in");

      mensagem("Preenchedo campos formulário ...");
      page.getElementById("username").setAttribute("value", USUARIO);
      page.getElementById("password").setAttribute("value", SENHA);
      page.getElementByName("commit").click();
    } catch (FailingHttpStatusCodeException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (MalformedURLException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (SecurityException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (IOException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private static void mensagem(String mensagem) {
    FileOutputStream fileOutputStream = null;
    PrintStream printStream = null;
    try {
      fileOutputStream = new FileOutputStream(new File("log.html"), true);
      printStream = new PrintStream(fileOutputStream);
      printStream.println(mensagem + "<br/>");
      System.out.println(mensagem);
    } catch (FileNotFoundException e) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    } finally {
      if (printStream != null) {
        printStream.close();
      }
    }
  }
}

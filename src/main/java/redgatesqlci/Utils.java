package redgatesqlci;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

public class Utils {
    public static boolean runSQLCIWithParams(AbstractBuild build, Launcher launcher, BuildListener listener, Collection<String> params)
    {
        // Check SQL CI is installed and get location.

        String sqlCiLocation = "";
        String allLocations = "";
        String[] possibleSqlCiLocations =
               {
                       System.getenv("DLMAS_HOME") +  "sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\DLM Automation Suite 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\SQL Automation Pack 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") + "\\Red Gate\\DLM Automation Suite 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") +  "\\Red Gate\\SQL Automation Pack 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") +  "\\Red Gate\\sqlCI\\sqlci.exe"
               } ;

        for(String possibleLocation : possibleSqlCiLocations)
        {
            if(new File(possibleLocation).isFile())
                sqlCiLocation = possibleLocation;
            allLocations = allLocations.concat(possibleLocation + "  ");
        }

        if(sqlCiLocation == "")
        {
            listener.error("SQL CI executable cannot be found. Checked " + allLocations + ".Please install Redgate SQL CI on this agent.");
            return false;
        }

        // Set up arguments

        ArrayList<String> procParams = new ArrayList<String>();
        procParams.add(sqlCiLocation);
        procParams.addAll(params);

        // Run SQL CI with parameters. Send output and error streams to logger.

        Proc proc = null;
        Launcher.ProcStarter procStarter = launcher.new ProcStarter();
        procStarter.cmds(procParams).stdout(listener.getLogger()).stderr(listener.getLogger()).pwd(build.getWorkspace());

        try {
            proc = launcher.launch(procStarter);
            int exitCode = proc.join();
            return exitCode == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException");
            return false;
        }
    }

    public static String constructPackageFileName(String packageName, int buildNumber)
    {
        return packageName + ".0." + buildNumber + ".nupkg";
    }
}

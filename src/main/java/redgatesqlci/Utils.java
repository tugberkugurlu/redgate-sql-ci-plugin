package redgatesqlci;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static boolean runSQLCIWithParams(AbstractBuild build, Launcher launcher, BuildListener listener, Collection<String> params)
    {
        // Check SQL CI is installed and get location.

        String sqlCiLocation = "";
        String allLocations = "";
        String[] possibleSqlCiLocations =
               {
                       System.getenv("DLMAS_HOME") +  "\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\DLM Automation Suite 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\SQL Automation Pack 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles") + "\\Red Gate\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") + "\\Red Gate\\DLM Automation Suite 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") +  "\\Red Gate\\SQL Automation Pack 1\\sqlCI\\sqlci.exe",
                       System.getenv("ProgramFiles(X86)") +  "\\Red Gate\\sqlCI\\sqlci.exe"
               } ;

        for(String possibleLocation : possibleSqlCiLocations)
        {
            if(new File(possibleLocation).isFile()) {
                sqlCiLocation = possibleLocation;
                break;
            }
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

        String longString = sqlCiLocation;

        // Here we do some parameter fiddling. Existing quotes must be escaped with three slashes
        // Then, we need to surround the part on the right of the = with quotes iff it has a space.
        for(String param : params)
        {
            // Trailing spaces can be a problem, so trim string.
            String fixedParam = param.trim();

            // Put 3 slashes before quotes (argh!!!!)
            if(fixedParam.contains("\""))
                fixedParam = fixedParam.replace("\"", "\\\\\\\"");

            // If there are spaces, surround bit after = with quotes
            if(fixedParam.contains(" "))
            {
                int equalsPlace = fixedParam.indexOf("=");
                fixedParam = fixedParam.substring(0, equalsPlace + 1) +  "\\\"" + fixedParam.substring(equalsPlace + 1, fixedParam.length()) + "\\\"";
            }

            procParams.add(param);
            longString += " " + fixedParam;
        }

        // Run SQL CI with parameters. Send output and error streams to logger.Map<String, String> vars = new HashMap<String, String>();
        Map<String, String> vars = new HashMap<String, String>();
        vars.putAll(build.getBuildVariables());


        Proc proc = null;
        Launcher.ProcStarter procStarter = launcher.new ProcStarter();

        // Set process environment variables to system environment variables. This shouldn't be necessary!
        EnvVars envVars = new EnvVars();
        try {
            envVars = build.getEnvironment(listener);
            vars.putAll(envVars);
        }
        catch (java.io.IOException e) {}
        catch (java.lang.InterruptedException e) {}
        procStarter.envs(vars);

        procStarter.cmdAsSingleString(longString).stdout(listener.getLogger()).stderr(listener.getLogger()).pwd(build.getWorkspace());

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

    public static String constructPackageFileName(String packageName, String buildNumber)
    {
        return packageName + "." + buildNumber + ".nupkg";
    }

}

package com.pinternals.alc;
import java.io.File;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;


public class Main {
	private static Logger log = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		
		CommandLine cmd = null;
		Options opts = new Options();
		opts.addOption("h", "help", false, "help");
		opts.addOption("v", "version", false, "print the version information and exit");
		opts.addOption("example", false, "Write configuration example (alc_example.xml or given one)");
		try {
			// look for non-JAR properties
			InputStream isLog = Main.class.getResourceAsStream("/logging.properties");
			if (isLog==null) {
				isLog = Main.class.getResourceAsStream("/com/pinternals/alc/logging.properties");
			} 
			if (isLog!=null)
				LogManager.getLogManager().readConfiguration(isLog);
			else
				LogManager.getLogManager().readConfiguration();
			log.finest("ALC is started");
			cmd = new PosixParser().parse(opts, args);
			LoginCheck.logInfo("Main001start");
			if (cmd.hasOption("help") || 
					( ( cmd.getOptions()==null || cmd.getOptions().length==0 ) &&
					  ( cmd.getArgs()==null || cmd.getArgs().length==0) ) ) { 
				new HelpFormatter().printHelp( "java -jar alc.jar", opts, true );
				return;
			} else if (cmd.hasOption("example")) {
				File example;
				if (cmd.getArgs()==null || cmd.getArgs().length==0)
					example = new File("alc_example.xml");
				else
					example = new File(cmd.getArgs()[0]);
				Config cfg = Config.createExample();
				cfg.writeConfig(example);
				cfg = Config.readConfig(example);
			} else if (cmd.hasOption("version")) { 
				System.out.println("http://pinternals.com/alc version " + LoginCheck.version);
				System.out.println("MDM client API version " + MdmClient.getClientAPIversion());
				System.out.println("RFC client version: UNAVAILABLE yet");
				System.out.println("FTP and HTTP clients: from Java Runtime version " + System.getProperty("java.runtime.version"));
//				for (Object k: System.getProperties().keySet()) {
//					System.out.println(k + ":" + System.getProperties().getProperty((String)k));
//				}
			} else if (cmd.getArgs()!=null && cmd.getArgs().length>0) {
				for (String c: cmd.getArgs()) {
					File f = new File(c);
					Config cfg = Config.readConfig(f);
					LoginCheck lc = new LoginCheck(cfg);
					lc.check();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

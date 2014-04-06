package com.pinternals.alc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.mdm.MdmException;
import com.sap.mdm.net.ConnectionException;
import com.sap.mdm.server.RepositoryIdentifier;
import com.sap.mdm.session.SessionException;

public class LoginCheck {
	public static String version = "0.02";
	private static ResourceBundle RES_MSG = ResourceBundle.getBundle("com.pinternals.alc.messages");
	private static Logger log = Logger.getLogger(LoginCheck.class.getName());

	/**
	 * @param key имя строки в messages.properties
	 * @param args параметры для форматирования
	 * @return строка для пользователя или для логов
	 */
	public static String format(String key, Object... args) {
		return new Formatter().format(RES_MSG.getString(key), args).toString();
	}

	static void logInfo(String res, Object ... args) {
		log.info(format(res, args));
	}
	static void logWarn(String res, Object ... args) {
		log.warning(format(res, args));
	}
	static void logSevere(String res, Object ... args) {
		log.severe(format(res, args));
	}
	
	Config cfg;
	LoginCheck (Config c) {
		this.cfg = c;
	}
	Map<String,SapMdmClient> mdmClients = new HashMap<String,SapMdmClient>();
	Map<String,SapNwClient> nwClients = new HashMap<String,SapNwClient>();
	Map<String,StandaloneClient> stClients = new HashMap<String, StandaloneClient>();
	
	
	void check1(StandaloneClient sc, Sys s) {
		for (Resource r: s.resource) {
			
		}
	}
	
	void check() {
		for (Sys s: cfg.system) {
			boolean hR = true;
			switch (s.nature) {
				case STANDALONE:
					StandaloneClient stCl = null;
					if (!stClients.containsKey(s.sid)) {
						stCl = new StandaloneClient(s.nature, s.connect, s.description);
						stClients.put(s.sid, stCl);
					} else
						stCl = stClients.get(s.sid);
					check1(stCl, s);
					break;
				case SAP_MDM:
					SapMdmClient mdmCl = null;
					if (!mdmClients.containsKey(s.sid)) {
						mdmCl = new SapMdmClient(s);
						mdmClients.put(s.sid, mdmCl);
					} else
						mdmCl = mdmClients.get(s.sid);

					try {
						hR = InetAddress.getByName(s.connect).isReachable(10 * 1000); // 10s
						if (!hR)
							logSevere("LoginCheck011unk", s.connect);
						else {
							// server connect without user
							mdmCl.simpleConnect();
							Set<String> rS = new HashSet<String>();
							String rA = "";
							for (RepositoryIdentifier r: mdmCl.reps) {
								rS.add(r.getName());
								rA += r.getName() + " ";
							}
							logInfo("LoginCheck009ri", s.connect, rA);

							for (Resource r: s.resource) {
								if (!rS.contains(r.name)) {
									logWarn("LoginCheck012miss", s.connect, r.name);
								}
								for (Check c: r.check) {
									try {
										mdmCl.connectStandaloneApp(s, r, c.acc, c.locale);
										logInfo("LoginCheck010ch", mdmCl.ctx.toString());
									} catch (ConnectionException ce) {
										logSevere("LoginCheck02connection", ce.getMessage() );
									} catch (SessionException se) {
										logSevere("LoginCheck03session", se.getMessage());
									} catch (MdmException me) {
										logSevere("LoginCheck015oth", me.getMessage());
									}
								}
							}
						}
					} catch (IOException uhe) {
						logSevere("LoginCheck011unk", s.connect);
					} 
					break;
				case SAP_NW:
					SapNwClient nwCl = null;
					if (!nwClients.containsKey(s.sid)) {
						nwCl = new SapNwClient(s);
						stClients.put(s.sid, nwCl);
					} else
						nwCl = nwClients.get(s.sid);
					nwCl.check();
					break;
				default:
					break;
			}
		}
	}

}

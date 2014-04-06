package com.pinternals.alc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

// Configuration file handling
// v0.01 -- very SAP MDM specific ("repository" as an resource)
// v0.02 -- idea changed from MDM to common resource polling 

enum Auth {
	AD, SSL, passwd, none;
}
enum Nature {
	SAP_NW, SAP_MDM, STANDALONE;
}
enum Protocol {
	FTP, HTTP, HTTPS, JDBC, MDM, RFC, SFTP;
}
enum Type {
	URL, Repository, mandt;
}


@Namespace(reference=Config.namespaceALC)
class Account {
	Sys system = null;
	Resource res = null;
	Account() { super(); }

	@Element(required=false)
	@Namespace(reference=Config.namespaceALC)
	String description = "";
	@Attribute(required=true)
	String login="";
	@Attribute(required=false)
	String passwd="";
}

@Namespace(reference=Config.namespaceALC)
class Check {
	Sys system = null;
	Account acc = null;
	Resource res = null;
	Check() {super();}

	@Element(required=false)
	@Namespace(reference=Config.namespaceALC)
	String description = "";

	@Attribute (required=true)
	String login = null; 

	@Attribute (required=false)
	String locale = null; 

}

@Namespace(reference=Config.namespaceALC)
class Resource {
	Resource() {super();}
	Sys system = null;

	@Attribute(required=true)
	String name = null;

	@Attribute(required=true)
	Type type = null;

	@Attribute(required=false)
	String locale = null;

	@Attribute (required=false)
	Auth auth;
	@Attribute (required=false)
	Protocol protocol;

	@Element(required=false)
	@Namespace(reference = Config.namespaceALC)
	String description = "";

	@ElementList(type=Account.class, inline=true, required=false)
	@Namespace(reference=Config.namespaceALC)
	List<Account> account = new ArrayList<Account>();

	@ElementList(type=Check.class, inline=true, required=false)
	@Namespace(reference=Config.namespaceALC)
	List<Check> check = new ArrayList<Check>();
}

@Root(name="system")
@Namespace(reference=Config.namespaceALC)
class Sys {
	Sys() {super();}

	@Element(required=false)
	@Namespace(reference=Config.namespaceALC)
	String description = null;

	@ElementList(type=Account.class, inline=true, required=false)
	@Namespace(reference=Config.namespaceALC)
	List<Account> account = new ArrayList<Account>();

	@ElementList(type=Resource.class, inline=true, required=false)
	@Namespace(reference=Config.namespaceALC)
	List<Resource> resource = new ArrayList<Resource>();

	@ElementList(type=Check.class, inline=true, required=false)
	@Namespace(reference=Config.namespaceALC)
	List<Check> check = new ArrayList<Check>();

	@Attribute (required=false)
	Nature nature;

	@Attribute (required=false)
	String sid, version;

	@Attribute (required=true)
	String connect;
}

@Root
@Namespace(reference=Config.namespaceALC, prefix="x")
public class Config {
	static final String namespaceALC = "http://pinternals.com/alc";
	private static Logger log = Logger.getLogger(Config.class.getName());
	
	@Attribute(required=false)
	@Namespace(reference="http://www.w3.org/2001/XMLSchema-instance")
	String schemaLocation;
	
	@Element(required=false)
   	@Namespace(reference=Config.namespaceALC)
   	String description;

   	@ElementList(type=Sys.class, inline=true, required=false)
   	@Namespace(reference=Config.namespaceALC)
   	List<Sys> system = new ArrayList<Sys>();
   	Config() {super();}

	static Config readConfig(File f) throws Exception {
		Config cf = null;
		Persister pers = new Persister();
		cf = pers.read(Config.class, f);
		log.fine(LoginCheck.format("Config002read", f.getAbsolutePath() ));
		
		// пост-обработка после чтения
		for (Sys s: cf.system) {
			Map<String,Account> x = new HashMap<String,Account>(), y = new HashMap<String,Account>();
			
			for (Account a: s.account) {
				a.system = s;
				a.res = null;
				x.put(a.login, a);
			}
			for (Check c: s.check) { 
				c.system = s;
				c.acc = x.get(c.login);
				assert c.acc!=null;
//				c.res = null;
			}
			for (Resource r: s.resource) {
				y.clear();
				for (Account a: r.account) {
					a.system = s;
					a.res = r;
					y.put(a.login, a);
				}
				for (Check c: r.check) {
					c.system = s;
					c.acc = y.get(c.login);
					c.acc = c.acc!=null ? c.acc : x.get(c.login);
					assert c.acc!=null;
//					c.res = r;
				}
			}
		}

		log.fine(LoginCheck.format("Config003read", f.getAbsolutePath() ));
		return cf;
	}
	
	void writeConfig(File f) throws Exception {
		Persister pers = new Persister();
		pers.write(this, f);
	}

	static Config createExample() throws Exception {
		Config ex = new Config();
		ex.description = "Тестовое описание";
//		Sys s = new Sys("MDM", "MDV", "hostname", "MDM for dev");
//		ex.system.add(s);
//		Region ru = new Region(MdmClient.locRU);
//		Repository classifiers = new Repository(s, "ASDW", "All but Quake", ru);
//		Account a = new Account(classifiers, "mdmpi", "-mdmpipwd-", null);
//		Check c = new Check(a, ru);
//		classifiers.check.add(c);
//
//		Repository kladr = new Repository(s, "QUAKE", null, ru);
//		Account a2 = new Account(kladr, "mdmpi", "-mdmpipwd-", null);
//		Check c2 = new Check(a2, ru);
//		kladr.check.add(c2);

		Sys j = new Sys();
		j.sid = "PIJ";
		j.connect = "http://sapsrv010:50600";
		j.nature = Nature.SAP_NW;
		ex.system.add(j);

//		Account gavriil = new Account("Gavriil", "AAaBBbCc=", "Good person");
//		j.account.add(gavriil);
//		Account devil = new Account("devil", "tobeopposite", "Not a good person");
//		j.account.add(devil);

//		Resource ume = new Resource(j, "/useradmin", null);
//		ume.auth = "webform731";
//		j.resource.add(ume);
//		ume.check.add(new Check(gavriil));

//		Resource sld = new Resource(j, "/sld", null);
//		sld.auth = "webform731";
//		j.resource.add(sld);
//		sld.check.add(new Check(gavriil));
//
//		Resource sqrep = new Resource(j, "/rep/support/SimpleQuery", null);
//		sqrep.auth = "webform731";
//		j.resource.add(sqrep);
//		sqrep.check.add(new Check(gavriil));
//		sqrep.check.add(new Check(devil));
//
//		Resource sqdir = new Resource(j, "/dir/support/SimpleQuery", null);
//		sqdir.auth = "webform731";
//		j.resource.add(sqdir);
//		sqdir.check.add(new Check(gavriil));
//		sqdir.check.add(new Check(devil));

		return ex;
	}
}

package com.pinternals.alc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.core.Persister;

// Configuration file handling

@Namespace(reference="http://pinternals.com/alc")
class Account {
	Repository rep = null;
//	Mandt mandt=null;
	Account() {super();}
	Account(Repository r, String login, String password, String description) {
		this.rep = r;
		this.login = login;
		this.pwd = password;
		if (this.rep!=null) this.rep.account.add(this);
	}

	@Attribute
	String login = "", pwd="";
	
	static Account findByLogin(List<Account> al, String login) {
		for (Account a: al) if (login.equals(a.login)) return a;
		return null;
	}
}

@Namespace(reference="http://pinternals.com/alc")
class Check {
	@Attribute (required=true)
	String login; 

	@Attribute (required=false)
	String region=null, report=null; 

	Check() {
		super();
	}
	Check(Account a, Region l) {
		this.acc = a;
		this.reg = l;
		this.login  = a.login;
		this.region = l.name;
	}
	void sync(Repository r) {
		this.reg = new Region(region);
		this.acc = Account.findByLogin(r.account, login);
//		this.report
	}

	Region reg = null;
	Account acc = null;
}

@Namespace(reference="http://pinternals.com/alc")
class Region {
	@Text
	String name;
	Region() {super();}
	Region(String s) {
		this.name = s;
	}
}

@Namespace(reference="http://pinternals.com/alc")
class Mandt {
	Sys sys = null;
	Mandt() {super();}
	Mandt(Sys ss, String number) {
		this.sys = ss;
		this.number = number;
	}

	@Attribute(required=true)
	String number="";
}


@Namespace(reference="http://pinternals.com/alc")
class Repository {
	Repository() {super();}
	Sys system = null;
	Repository(Sys s, String name, String descr, Region ... regs) {
		this.system = s;
		this.name = name;
		this.description = descr;
		for (Region lc: regs) 
			region.add(lc);
		if (s!=null) this.system.repository.add(this);
	}

	@Attribute
	String name = null;

	@Element(required=false)
	@Namespace(reference="http://pinternals.com/alc")
	String description = "";

	@ElementList(type=Region.class, inline=true, required=true, name="region", entry="region")
	@Namespace(reference="http://pinternals.com/alc")
	List<Region> region = new ArrayList<Region>();

	@ElementList(type=Account.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Account> account = new ArrayList<Account>();
	
	@ElementList(type=Check.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Check> check = new ArrayList<Check>();
}

@Root(name="system")
@Namespace(reference="http://pinternals.com/alc")
class Sys {
	Sys() {super();}
	Sys(String nat, String sid, String host, String description) {
		this.nature = ENature.valueOf(nat);
		this.sid = sid;
		this.host = host;
		this.description = description;
	}

	@Element(required=false)
	@Namespace(reference="http://pinternals.com/alc")
	String description = null;

	@ElementList(type=Account.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Account> account = new ArrayList<Account>();

	@ElementList(type=Repository.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Repository> repository = new ArrayList<Repository>();

	@ElementList(type=Mandt.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Mandt> mandt = new ArrayList<Mandt>();

	@ElementList(type=Check.class, inline=true, required=false)
	@Namespace(reference="http://pinternals.com/alc")
	List<Check> check = new ArrayList<Check>();

	@Attribute (required=false)
	ENature nature;

	@Attribute (required=false)
	String sid;

	@Attribute (required=false)
	String host;

	void addRepository(Repository r) {
		repository.add(r);
	}
}

@Root
@Namespace(reference="http://pinternals.com/alc", prefix="x")
public class Config {
	private static Logger log = Logger.getLogger(Config.class.getName());
	
	@Attribute(required=false)
	@Namespace(reference="http://www.w3.org/2001/XMLSchema-instance")
	String schemaLocation;
	
	@Element(required=false)
   	@Namespace(reference="http://pinternals.com/alc")
   	String description;

   	@ElementList(type=Sys.class, inline=true, required=false)
   	@Namespace(reference="http://pinternals.com/alc")
   	List<Sys> system = new ArrayList<Sys>();
   	Config() {super();}

	static Config readConfig(File f) throws Exception {
		Config cf = null;
		Persister pers = new Persister();
		cf = pers.read(Config.class, f);
		log.fine(LoginCheck.format("Config002read", f.getAbsolutePath() ));
		
		// пост-обработка после чтения
		for (Sys s: cf.system) 
			for (Repository r: s.repository) 
				for (Check c: r.check) 
					c.sync(r);

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
		Sys s = new Sys("MDM", "MDV", "hostname", "MDM for dev");
		ex.system.add(s);
		Region ru = new Region(MdmClient.locRU);
		Repository classifiers = new Repository(s, "ASDW", "All but Quake", ru);
		Account a = new Account(classifiers, "mdmpi", "-mdmpipwd-", null);
		Check c = new Check(a, ru);
		classifiers.check.add(c);

		Repository kladr = new Repository(s, "QUAKE", null, ru);
		Account a2 = new Account(kladr, "mdmpi", "-mdmpipwd-", null);
		Check c2 = new Check(a2, ru);
		kladr.check.add(c2);

		return ex;
	}
}

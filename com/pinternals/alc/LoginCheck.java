package com.pinternals.alc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Formatter;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sap.mdm.MdmException;
import com.sap.mdm.commands.CommandException;
import com.sap.mdm.commands.CreateServerSessionCommand;
import com.sap.mdm.commands.GetRunningRepositoryListCommand;
import com.sap.mdm.extension.session.ConnectionManagerEx;
import com.sap.mdm.ids.TableId;
import com.sap.mdm.net.ConnectionException;
import com.sap.mdm.net.MdmSSLException;
import com.sap.mdm.net.SimpleConnection;
import com.sap.mdm.net.SimpleConnectionFactory;
import com.sap.mdm.repository.RepositoryProperties;
import com.sap.mdm.repository.XMLSchemaProperties;
import com.sap.mdm.repository.commands.GetRepositoryPropertiesCommand;
import com.sap.mdm.repository.commands.GetXMLSchemaListCommand;
import com.sap.mdm.schema.FieldProperties;
import com.sap.mdm.schema.RepositorySchema;
import com.sap.mdm.schema.TableProperties;
import com.sap.mdm.schema.TableSchema;
import com.sap.mdm.schema.commands.GetRepositorySchemaCommand;
import com.sap.mdm.server.RepositoryIdentifier;
import com.sap.mdm.session.NonNetWeaverMdmDestinationProperties;
import com.sap.mdm.session.SessionException;
import com.sap.mdm.session.UserSessionContext;

enum ENature {
	MDM, J2EE_SAP, ABAP, JDBC, HTTP, FTP;
}

class MdmClient {
	private static Logger log = Logger.getLogger(MdmClient.class.getName());
	static String locRU = "Russian [RU]";
	static String locEN_US = "English [US]";
	static String appName = "Alc";

	UserSessionContext ctx = null;
	RepositoryIdentifier[] reps = null;
	NonNetWeaverMdmDestinationProperties destination = null;
	public static String getClientAPIversion() {
		return com.sap.mdm.util.BuildVersion.getBuildVersion();
	}
	public UserSessionContext connectStandaloneApp(
			Sys serv, Repository rep, Account acc, Region rg)
		throws SessionException, MdmSSLException, ConnectionException {
		assert serv != null;
		assert rep != null;
		assert acc != null;
		assert rg != null;

		ctx = connectStandaloneApp(serv.host, rep.name, rg.name, acc.login, acc.pwd,
				com.sap.mdm.commands.SetUnicodeNormalizationCommand.NORMALIZATION_COMPOSED, 
				false);
		log.finest( LoginCheck.format("MdmClient003connect", ctx.toString()) );
		return ctx;
	}
	
	public UserSessionContext connectStandaloneApp(
			String serverName,
			String repositoryName, String regionName,
			String repositoryUser, String repositoryPassword,
			int unicodeNormType, boolean useBlobCache)  
		throws SessionException, MdmSSLException, ConnectionException {
		/*
		 * Create an instance of NonNetWeaverMdmDestinationProperties
		 *(implementation of MdmDestinationProperties for non-NetWeaver
		 * destinations)
		 */
		destination = new NonNetWeaverMdmDestinationProperties(serverName, repositoryName, repositoryUser);
			 
		// Connect to MDS
		ctx = ConnectionManagerEx.connectWithUserSession(
			destination, repositoryPassword, appName, regionName,
	        null, unicodeNormType, useBlobCache, null);
		return ctx;
	}
	
	
	static XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
	static String nsALC = "http://pinternals.com/alc";
	static void writeAttr(XMLStreamWriter xmlw, String attrName, boolean value) 
		throws XMLStreamException {
			xmlw.writeAttribute(attrName, value?"1":"0");
	}
	static void doCheckRepositoryReportXML(OutputStream os, 
			RepositorySchema rs, XMLSchemaProperties[] xsp, RepositoryProperties rp) throws UnsupportedEncodingException, IOException, XMLStreamException {
		RepositoryIdentifier rif = rs.getRepositoryIdentifier();
		
		XMLStreamWriter xmlw = xmlof.createXMLStreamWriter(os);
		
		xmlw.setDefaultNamespace("");
		xmlw.writeStartDocument("cp1251", "1.0");

		xmlw.writeStartElement("k", "report", nsALC);
		xmlw.writeNamespace("k", nsALC);

		xmlw.writeStartElement(nsALC, "repository");
		xmlw.writeAttribute("name", rif.getName());
		if (!"".equals(rp.getDescription())) 
			xmlw.writeAttribute("description", rp.getDescription());
		xmlw.writeCharacters("\n");

		for (TableId tid: rs.getTableIds()) {
			TableSchema ts = rs.getTableSchema(tid);
			TableProperties tp = ts.getTable();
			xmlw.writeStartElement(nsALC, "table");
			xmlw.writeAttribute("name", tp.getName().toString());
			xmlw.writeAttribute("code", tp.getCode());
			xmlw.writeAttribute("id", Integer.toString(tid.id));

			if (!"".equals(tp.getDescription())) 
				xmlw.writeAttribute("description", tp.getDescription());
			
			xmlw.writeCharacters("\n");
			for (FieldProperties fid: ts.getFields()) {
				xmlw.writeStartElement(nsALC, "field");
				xmlw.writeAttribute("id", Integer.toString(fid.getId().id));
				xmlw.writeAttribute("name", fid.getName().toString() );
				xmlw.writeAttribute("code", fid.getCode() );
				if (!"".equals(fid.getDescription())) xmlw.writeAttribute("description", fid.getDescription() );

				xmlw.writeAttribute("typeName", fid.getTypeName() );
				xmlw.writeAttribute("position", Integer.toString(fid.getPosition()) );
				xmlw.writeAttribute("sortIndex", Integer.toString(fid.getSortIndex()) );
				
				
				
				xmlw.writeStartElement("is");
				writeAttr(xmlw, "calculated", fid.isCalculated());
				writeAttr(xmlw, "editable", fid.isEditable());
				writeAttr(xmlw, "freeFormSearchable", fid.isFreeFormSearchable());
				writeAttr(xmlw, "hierarchyLookup", fid.isHierarchyLookup());
				writeAttr(xmlw, "keywordIndexable", fid.isKeywordIndexable());
				writeAttr(xmlw, "largeObjectLookup", fid.isLargeObjectLookup());
				writeAttr(xmlw, "lookup", fid.isLookup());
				writeAttr(xmlw, "modifyOnce", fid.isModifyOnce());
				writeAttr(xmlw, "multiLingual", fid.isMultiLingual());
				writeAttr(xmlw, "multiValued", fid.isMultiValued());
				writeAttr(xmlw, "picklistSearchable", fid.isPicklistSearchable());
				writeAttr(xmlw, "qualified", fid.isQualified());
				writeAttr(xmlw, "qualifier", fid.isQualifier());
				writeAttr(xmlw, "qualifierCached", fid.isQualifierCached());
				writeAttr(xmlw, "required", fid.isRequired());
				writeAttr(xmlw, "showInSearchTab", fid.isShowInSearchTab());
				writeAttr(xmlw, "sortable", fid.isSortable());
				writeAttr(xmlw, "taxonomyLookup", fid.isTaxonomyLookup());
				writeAttr(xmlw, "tuple", fid.isTuple());
				xmlw.writeEndElement();
				
				xmlw.writeEndElement();
				xmlw.writeCharacters("\n");
			}
			
			xmlw.writeEndElement();
		}
		xmlw.writeEndElement();
		
		xmlw.writeEndElement();
		xmlw.writeEndDocument();

		xmlw.close();
		os.close();
		
//		os.write(s.getBytes("UTF-8"));
	}
	
	void getRepositoryInformation(String repname, File f) {
		RepositorySchema rs = null;
		XMLSchemaProperties[] xsp = null;
		RepositoryProperties rp = null;
		for (RepositoryIdentifier rif: reps) if (rif.getName().equals(repname)) {
			GetRepositoryPropertiesCommand gRPC = null;
			GetRepositorySchemaCommand gRSC = null;
			GetXMLSchemaListCommand gXSC = null;
			try {
				gRPC = new GetRepositoryPropertiesCommand(ctx);
				gRPC.execute();
				rp = gRPC.getRepositoryProperties();
				gRSC = new GetRepositorySchemaCommand(ctx);
				gRSC.execute();
				rs = gRSC.getRepositorySchema();
				gXSC = new GetXMLSchemaListCommand(ctx); 
				gXSC.execute();
				xsp = gXSC.getXMLSchemas();
				FileOutputStream fos = new FileOutputStream(f);
				MdmClient.doCheckRepositoryReportXML(fos, rs, xsp, rp);
			} catch (ConnectionException ce) {
				ce.printStackTrace();
			} catch (CommandException ce) {
				ce.printStackTrace();
			} catch (FileNotFoundException ce) {
				ce.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}
	
	void simpleConnect(String mdsName) {
        SimpleConnection conAccessor = null;
//        Connection c = null;
        CreateServerSessionCommand servSessCmd = null;
//        GetServerVersionCommand gvCmd = null;
		try {
			conAccessor = SimpleConnectionFactory.getInstance(mdsName);
			servSessCmd = new CreateServerSessionCommand(conAccessor);
			servSessCmd.execute();
			
			GetRunningRepositoryListCommand cmdRepList = new GetRunningRepositoryListCommand(conAccessor);
			cmdRepList.execute();
			reps = cmdRepList.getRepositories();

			// Информация о пользователях и о версии требует установленного авторизованного соединения
			// SimpleConnection здесь не работает 
//			GetUser sCommand getUsrsCmd = new GetUsersCommand(conAccessor); 
//			gvCmd = new GetServerVersionCommand(c);
//			gvCmd.execute(arg0)
//			c = conAccessor.reserveConnection("");
		} catch (Exception x) {
			x.printStackTrace();
		}
		
	}
}

public class LoginCheck {
	public static String version = "0.01";
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

	void check() {
		MdmClient mdmCl = new MdmClient();
		for (Sys s: cfg.system) {
			boolean hR = true;
			switch (s.nature) {
				case MDM:
					try {
						hR = InetAddress.getByName(s.host).isReachable(10 * 1000); // 10s
						if (!hR)
							logSevere("LoginCheck011unk", s.host);
						else {
							mdmCl.simpleConnect(s.host);
							Set<String> rS = new HashSet<String>();
							String rA = "";
							for (RepositoryIdentifier r: mdmCl.reps) {
								rS.add(r.getName());
								rA += r.getName() + " ";
							}
							logInfo("LoginCheck009ri", s.host, rA);
							
							for (Repository r: s.repository) {
								if (!rS.contains(r.name)) {
									logWarn("LoginCheck012miss", s.host, r.name);
								}
								for (Check c: r.check) {
									try {
										mdmCl.connectStandaloneApp(s, r, c.acc, c.reg);
										logInfo("LoginCheck010ch", mdmCl.ctx.toString());
										if (c.report!=null) {
											File f = new File(c.report);
											mdmCl.getRepositoryInformation(r.name, f);
										}
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
						logSevere("LoginCheck011unk", s.host);
					} 
					break;
				case HTTP:
					break;
				case ABAP:
					break;
				case FTP:
					break;
				case JDBC:
					break;
				case J2EE_SAP:
					break;
				default:
					break;
			}
		}
	}
}

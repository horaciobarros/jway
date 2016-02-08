package jway;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

public class CriaArquiteturaNova {
	private Map<String, CampoFk> mapCamposFk = new HashMap<String, CampoFk>();
	private Map<String, String> mapEntidades = new HashMap<String, String>();
	private Connection conn;
	private String space = "\t";
	private String nomePacote;
	private String modelPath;
	private String servicePath;
	private String serviceImplPath;
	private String daoPath;
	private String daoImplPath;
	private String beanPath;
	private String viewPath;
	private DatabaseMetaData dbmd;
	private boolean isPostgresql = false;
	private String nomeBanco = "bloqueio";
	private String user = "postgres";
	private String password = "postgres";

	public static void main(String[] args) {
		CriaArquiteturaNova c = new CriaArquiteturaNova();
		c.processa();

	}

	public CriaArquiteturaNova() {
		// registrar o Driver JDBC do banco de dados, neste caso estou usando o
		// da Oracle
		try {
			if (isPostgresql) {

				DriverManager.registerDriver(new org.postgresql.Driver());
				conn = DriverManager.getConnection(
						"jdbc:postgresql://localhost:5432/" + nomeBanco, user,
						password);

				// recuperar a classe DatabaseMetadaData a partir da conexao
				// criada
				dbmd = conn.getMetaData();
			} else {
				DriverManager.registerDriver(new com.mysql.jdbc.Driver());
				conn = DriverManager.getConnection(
						"jdbc:mysql://107.161.176.58:3306/fitapp", "fitapp",
						"abc123#");
				// recuperar a classe DatabaseMetadaData a partir da conexao
				// criada
				dbmd = conn.getMetaData();

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void processa() {
		try {
			nomePacote = "br.com.jway"; // isto vai ser informado na tela
			montaNomePastas();
			criaPastas();

			String[] tableTypes = { "TABLE" };

			DatabaseMetaData metaData;
			metaData = conn.getMetaData();
			ResultSet listaTabelas = metaData.getTables(null, null, "%",
					tableTypes);
			// String nomeTabela = "unidade_orcamentaria";

			System.out.println("Versao do Driver JDBC = "
					+ dbmd.getDriverVersion());
			System.out.println("Versao do Banco de Dados = "
					+ dbmd.getDatabaseProductVersion());
			System.out.println("Suporta Select for Update? = "
					+ dbmd.supportsSelectForUpdate());
			System.out.println("Suporta Transacoes? = "

			+ dbmd.supportsTransactions());

			// retornar todos os schemas(usuarios) do Banco de Dados
			ResultSet r2 = dbmd.getSchemas();
			while (r2.next()) {
				System.out.println("SCHEMA DO BD = " + r2.getString(1));
			}

			while (listaTabelas.next()) {
				String nomeTabela = listaTabelas.getString("TABLE_NAME");
				armazenaFks(nomeTabela);
				criaEntidade(nomeTabela);
				criaDao(nomeTabela);
				criaService(nomeTabela);
				criaManagedBean(nomeTabela);
				criaViewJsf(nomeTabela);

			}
			criaAmbiente();
			System.out.println(" ----- Processo encerrado ----");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void criaAmbiente() {
		// TODO Auto-generated method stub

	}

	private void montaNomePastas() {
		String caminhoPadrao = "/TEMP/src/main/java/"+nomePacote.replace(".", "/");
		modelPath = caminhoPadrao + "/model/";
		daoPath = caminhoPadrao + "/dao/";
		daoImplPath = caminhoPadrao + "/dao/impl/";
		servicePath = caminhoPadrao + "/service/";
		serviceImplPath = caminhoPadrao + "/service/impl/";
		beanPath = caminhoPadrao + "/view/";
		viewPath = "/TEMP/src/main/webapp/view/";

	}

	private void armazenaFks(String nomeTabela) {
		// Buscando as foreign keys da tabela aldeia
		DatabaseMetaData metaData;
		try {
			metaData = conn.getMetaData();
			ResultSet foreignKeys = metaData.getImportedKeys(conn.getCatalog(),
					null, nomeTabela);

			while (foreignKeys.next()) {
				String fkTableName = foreignKeys.getString("FKTABLE_NAME");
				String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
				String pkTableName = foreignKeys.getString("PKTABLE_NAME");
				String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
				// System.out.println(fkTableName + "." + fkColumnName + " -> "
				// + pkTableName + "." + pkColumnName);
				CampoFk campoFk = new CampoFk();
				campoFk.setFkColumnName(fkColumnName);
				campoFk.setFkTableName(fkTableName);
				campoFk.setPkColumnName(pkColumnName);
				campoFk.setPkTableName(pkTableName);
				mapCamposFk.put(fkColumnName.toUpperCase(), campoFk);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void criarPasta(String pasta) {
		File diretorio;
		try {
			diretorio = new File(pasta);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - "
					+ diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"Erro ao criar o diretorio model");
			System.out.println(ex);
			ex.printStackTrace();
		}
	}

	private void criaPastas() {
		criarPasta(modelPath);
		criarPasta(serviceImplPath);
		criarPasta(servicePath);
		criarPasta(daoImplPath);
		criarPasta(daoPath);
		criarPasta(beanPath);
		criarPasta(viewPath);
	}

	private void criaEntidade(String nomeTabela) throws SQLException {

		Statement stmt = conn.createStatement();
		// Tabela a ser analisada

		ResultSet rset = stmt.executeQuery("SELECT * from " + nomeTabela);

		ResultSetMetaData rsmd = rset.getMetaData();

		// retorna o numero total de colunas
		int numColumns = rsmd.getColumnCount();
		System.out.println("tabela " + nomeTabela + ": Total de Colunas = "
				+ numColumns);

		// criando a entidade
		String nomeEntidade = transformaNomeEntidade(nomeTabela);

		mapEntidades.put(nomeTabela, nomeEntidade);

		File fileEntidade = new File(modelPath + nomeEntidade + ".java");

		try {
			FileWriter fw = new FileWriter(fileEntidade);

			fw.write("package " + nomePacote + ".model; \n");

			fw.write("\n");

			fw.write("import java.io.Serializable;\n");
			fw.write("import javax.persistence.Column;\n");
			fw.write("import javax.persistence.Entity;\n");
			fw.write("import javax.persistence.GeneratedValue;\n");
			fw.write("import javax.persistence.GenerationType;\n");
			fw.write("import javax.persistence.Id;\n");
			fw.write("import javax.persistence.JoinColumn;\n");
			fw.write("import javax.persistence.ManyToOne;\n");
			fw.write("import javax.persistence.Table;\n");
			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import java.math.BigDecimal;\n");

			fw.write("import java.sql.Time;\n");
			fw.write("\n");

			fw.write("@Entity \n");
			fw.write("@Table(name=\"" + nomeTabela + "\")\n");

			fw.write("public class " + nomeEntidade
					+ " implements Serializable {\n");

			fw.write("\n");

			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");

			// definindo as colunas
			for (int i = 0; i < numColumns; i++) {

				fw.write("\n");

				if (rsmd.getColumnName(i + 1).equals("id")) {
					fw.write(space + "@Id\n");
					fw.write(space
							+ "@GeneratedValue(strategy = GenerationType.IDENTITY)\n");

				}

				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1)
						.toUpperCase())) { // se for uma fk
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1)
							.toUpperCase());

					fw.write(space + "@ManyToOne");
					fw.write("\n");
					fw.write(space + "@JoinColumn(name = \""
							+ fk.getFkColumnName() + "\")");
					fw.write("\n");

					fw.write(space
							+ "private "
							+ transformaNomeEntidade(fk.getPkTableName())
							+ " "
							+ transformaNomeColuna(fk.getPkTableName()
									.toLowerCase()) + ";\n");

				} else {
					fw.write(space + "@Column(name=\""
							+ rsmd.getColumnName(i + 1) + "\")\n");
					fw.write(space
							+ "private "
							+ transformaTipo(rsmd.getColumnTypeName(i + 1),
									rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase()
											.contains("id")) + " "
							+ transformaNomeColuna(rsmd.getColumnName(i + 1))
							+ ";\n");
				}

			}

			// gets e sets
			for (int i = 0; i < numColumns; i++) {
				fw.write("\n");
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1)
						.toUpperCase())) { // se for uma fk
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1)
							.toUpperCase());

					fw.write("\n");

					fw.write(space + "public  "
							+ transformaNomeEntidade(fk.getPkTableName()) + " "
							+ " " + "get"
							+ transformaNomeEntidade(fk.getPkTableName())
							+ "() { \n");
					fw.write(space + space + "return "
							+ transformaNomeColuna(fk.getPkTableName()) + ";\n");
					fw.write(space + "}\n");

					fw.write(space + "public void " + " " + "set"
							+ transformaNomeEntidade(fk.getPkTableName())

							+ "(" + transformaNomeEntidade(fk.getPkTableName())
							+ " " + transformaNomeColuna(fk.getPkTableName())
							+ ") { \n");
					fw.write(space + space + "this."
							+ transformaNomeColuna(fk.getPkTableName()) + " = "
							+ transformaNomeColuna(fk.getPkTableName()) + ";\n");
					fw.write(space + "}\n");

				} else {
					fw.write(space
							+ "public "
							+ transformaTipo(rsmd.getColumnTypeName(i + 1),
									rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase()
											.contains("id"))
							+ " "
							+ "get"
							+ transformaNomeColunaPrimeiroCaracterMaiusculo(rsmd
									.getColumnName(i + 1)) + "() { \n");
					fw.write(space + space + "return "
							+ transformaNomeColuna(rsmd.getColumnName(i + 1))
							+ ";\n");
					fw.write(space + "}\n");

					fw.write(space
							+ "public void "
							+ " "
							+ "set"
							+ transformaNomeColunaPrimeiroCaracterMaiusculo(rsmd
									.getColumnName(i + 1))
							+ "("
							+ transformaTipo(rsmd.getColumnTypeName(i + 1),
									rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase()
											.contains("id")) + " "
							+ transformaNomeColuna(rsmd.getColumnName(i + 1))
							+ ") { \n");
					fw.write(space + space + "this."
							+ transformaNomeColuna(rsmd.getColumnName(i + 1))
							+ " = "
							+ transformaNomeColuna(rsmd.getColumnName(i + 1))
							+ ";\n");
					fw.write(space + "}\n");
				}

			}

			fw.write("}"); // final da classe

			fw.flush();
			fw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String transformaNomeEntidade(String nomeTabela) {

		String aux = nomeTabela.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = new String();

		for (int i = 0; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase()
					+ pedacos[i].substring(1);
		}

		return aux;
	}

	private static String transformaNomeColunaPrimeiroCaracterMaiusculo(
			String columnName) {

		String aux = columnName.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = pedacos[0].substring(0, 1).toUpperCase()
				+ pedacos[0].substring(1);

		for (int i = 1; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase()
					+ pedacos[i].substring(1);
		}
		return aux;

	}

	private static String transformaNomeColuna(String columnName) {

		String aux = columnName.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = pedacos[0];

		for (int i = 1; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase()
					+ pedacos[i].substring(1);
		}
		return aux;

	}

	private static String transformaTipo(String tipo, int decimais,
			boolean campoId) {
		tipo = tipo.toLowerCase();

		// para o postgresql

		if (tipo.equals("numeric")) {
			if (decimais == 0)
				return ("long");
			else
				return ("BigDecimal");
		}
		if (tipo.equals("varchar"))
			return "String";
		if (tipo.contains("date"))
			return "Date";
		if (tipo.contains("int")) {
			return "long";
		}
		if (tipo.equals("timestamp")) {
			return "Date";
		}
		if (tipo.equals("bool")) {
			return "boolean";
		}

		if (tipo.contains("bool")) {
			return "boolean";
		}

		if (tipo.equals("bpchar")) {
			return "String";
		}

		if (tipo.equals("decimal")) {
			return "Double";
		}

		if (tipo.equals("blob")) {
			return "byte[]";
		}

		if (tipo.equals("text")) {
			return "String";
		}
		
		if (tipo.equals("bit")) {
			return "int";
		}
		
		if (tipo.equals("time")) {
			return "Time";
		}

		return tipo;

	}

	private void criaDao(String nomeTabela) {
		// criando a interface
		String nomeEntidade = transformaNomeEntidade(nomeTabela);
		String nomeInterface = nomeEntidade + "Dao";

		File fileInterface = new File(daoPath + nomeInterface + ".java");

		String space = "\t";
		try {
			FileWriter fw = new FileWriter(fileInterface);

			fw.write("package " + nomePacote + ".dao; \n");

			fw.write("\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");

			fw.write("\n");

			fw.write("public interface " + nomeInterface + "  {\n");

			fw.write("\n");

			fw.write(space + "List<" + nomeEntidade + "> list();\n");

			fw.write(space + nomeEntidade + " read(long id);\n");

			fw.write(space + "void create(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n");

			fw.write(space + nomeEntidade + " update(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n");

			fw.write(space + "void delete(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n");

			fw.write(space + "void delete(long id);\n");

			fw.write("}"); // final da interface

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		criaDaoImpl(nomeEntidade, nomeInterface);

	}

	private void criaDaoImpl(String nomeEntidade, String nomeInterface) {
		File fileDaoImpl = new File(daoImplPath + nomeInterface + "Impl.java");

		String space = "\t";
		try {
			FileWriter fw = new FileWriter(fileDaoImpl);

			fw.write("package " + nomePacote + ".dao.impl; \n");

			fw.write("\n");

			fw.write("import java.util.List;\n");
			fw.write("import javax.inject.Named;\n");
			fw.write("import javax.persistence.EntityManager;\n");
			fw.write("import javax.persistence.PersistenceContext;\n");
			fw.write("import org.springframework.transaction.annotation.Propagation;\n");
			fw.write("import org.springframework.transaction.annotation.Transactional;\n");
			fw.write("import " + nomePacote + ".dao." + nomeInterface + ";\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".dao.*;\n");
			fw.write("import com.uaihebert.uaicriteria.UaiCriteria;\n");

			fw.write("\n");

			fw.write("@Named \n");
			fw.write("public class " + nomeInterface + "Impl implements "
					+ nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");
			fw.write(space + "@PersistenceContext \n");
			fw.write(space + "protected EntityManager em;\n");
			fw.write("\n");
			fw.write(space + "UaiCriteria<" + nomeEntidade
					+ "> uaiCriteria;\n\n");
			fw.write(space + "@Override\n");
			fw.write(space + " public List<" + nomeEntidade + "> list() {\n");
			fw.write(space + space + "StringBuilder jpql = new StringBuilder()\n"); //
			fw.write(space + space + space + ".append(\"SELECT x\") \n");
			fw.write(space + space + space + ".append(\"FROM \" + " + nomeEntidade + ".class.getName() + \" x \") //\n");
			fw.write(space + space + space + ".append(\"ORDER BY x.id ASC \");\n");
			fw.write(space + space + "return em.createQuery(jpql.toString(), " + nomeEntidade + ".class).getResultList();\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@Override\n");
			fw.write(space + "public " + nomeEntidade + " read(long id) {\n");
			fw.write(space + space + "return em.find(" + nomeEntidade + ".class, id);\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@Override\n");
			fw.write(space + "@Transactional(propagation = Propagation.MANDATORY)\n");
			fw.write(space + "public void create(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ") {\n");
			fw.write(space + space + "em.persist(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@Override\n");
			fw.write(space + "@Transactional(propagation = Propagation.MANDATORY)\n");
			fw.write(space + "public " + nomeEntidade + " update(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ") {\n");
			fw.write(space + space + "return em.merge(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@Override\n");
			fw.write(space + "@Transactional(propagation = Propagation.MANDATORY)\n");
			fw.write(space + "public void delete(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ") {\n");
			fw.write(space + space + "em.remove(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@Override\n");
			fw.write(space + "@Transactional(propagation = Propagation.MANDATORY)\n");
			fw.write(space + "public void delete(long id) {\n");
			fw.write(space + space + "" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + " = em.getReference(" + nomeEntidade + ".class, id);\n");
			fw.write(space + space + "delete(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "\n");

			fw.write("}"); // final da classe dao

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void criaService(String nomeTabela) {
		// criando a interface
		String nomeEntidade = transformaNomeEntidade(nomeTabela);
		String nomeInterface = nomeEntidade + "Service";

		File fileInterface = new File(servicePath + nomeInterface + ".java");

		String space = "\t";
		try {
			FileWriter fw = new FileWriter(fileInterface);

			fw.write("package " + nomePacote + ".service; \n");

			fw.write("\n");
			fw.write("import java.io.Serializable;\n");
			fw.write("import java.util.List;\n");

			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");

			fw.write("\n");

			fw.write("public interface " + nomeInterface
					+ " extends Serializable {\n");

			fw.write("\n");

			fw.write(space + "public void create(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void delete(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void update(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public List<" + nomeEntidade + "> list();\n\n");
			
			fw.write(space + "public void delete(long id);\n\n");

			fw.write(space + "public " + nomeEntidade + " read(long id);\n\n");

			fw.write("}"); // final da interface

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		criaServiceImpl(nomeEntidade, nomeInterface);

	}

	private void criaServiceImpl(String nomeEntidade, String nomeInterface) {
		File fileServiceImpl = new File(serviceImplPath + nomeInterface
				+ "Impl.java");
		String nomeInterfaceDao = nomeEntidade + "Dao";

		String space = "\t";
		try {
			FileWriter fw = new FileWriter(fileServiceImpl);

			fw.write("package " + nomePacote + ".service.impl; \n");

			fw.write("\n");

			fw.write("import java.util.List;\n");
			fw.write("import javax.inject.Inject;\n");
			fw.write("import javax.inject.Named;\n\n");
			fw.write("import org.springframework.transaction.annotation.Propagation;\n");
			fw.write("import org.springframework.transaction.annotation.Transactional;\n\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".dao." + nomeInterfaceDao
					+ ";\n");
			fw.write("import " + nomePacote + ".service." + nomeInterface
					+ ";\n\n");

			fw.write("@Named\n");
			fw.write("public class " + nomeInterface + "Impl implements "
					+ nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");
			fw.write(space + "@Inject \n");
			fw.write(space + "private " + nomeEntidade + "Dao dao;\n");
			fw.write("\n");
			// --
			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public void create(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.create("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public void delete(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.delete("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public void update(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.update("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");
			
			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public " + nomeEntidade + " read(long id) {\n");
			fw.write(space + space + "return dao.read(id);\n");
			fw.write(space + "}\n\n");
			
			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public void delete(long id) {\n");
			fw.write(space + space + "dao.delete(id);\n");
			fw.write(space + "}\n\n");

			// --
			fw.write(space + "@Override\n");
			fw.write(space + "public List<" + nomeEntidade + "> list(){\n");
			fw.write(space + space + "return dao.list();\n ");

			fw.write(space + "}\n");

			// --
			fw.write("}"); // final da classe dao

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void criaManagedBean(String nomeTabela) {
		String nomeEntidade = mapEntidades.get(nomeTabela);
		File fileBean = new File(beanPath + nomeEntidade + "Bean.java");

		String space = "\t";
		try {
			FileWriter fw = new FileWriter(fileBean);

			fw.write("package " + nomePacote + ".view; \n");

			fw.write("\n");
			fw.write("import java.io.Serializable;\n"
					+ "import java.util.List;\n"
					+ "import javax.annotation.PostConstruct;\n"
					+ "import javax.annotation.PreDestroy;\n");

			fw.write("import javax.faces.bean.ManagedBean;\n"
					+ "import javax.faces.bean.ViewScoped;\n"
					+ "import javax.inject.Inject;\n"
					+ "import org.apache.commons.logging.Log;\n"
					+ "import org.apache.commons.logging.LogFactory;\n"
					+ "import org.springframework.beans.factory.annotation.Autowire;\n"
					+ "import org.springframework.web.context.support.SpringBeanAutowiringSupport;\n");

			fw.write("import " + nomePacote + ".service." + nomeEntidade
					+ "Service;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".util.FacesUtils;\n");
			fw.write("\n");

			fw.write("@ManagedBean\n");
			fw.write("@ViewScoped\n");

			fw.write("public  class "
					+ nomeEntidade
					+ "Bean"
					+ " extends SpringBeanAutowiringSupport implements Serializable "
					+ "{\n");
			fw.write("\n");

			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");

			fw.write(space
					+ "protected static final Log log = LogFactory.getLog("
					+ nomeEntidade + "Bean.class);\n");
			fw.write("\n");
			fw.write(space + "@Inject\n");
			fw.write(space + "private " + nomeEntidade
					+ "Service service;\n");
			fw.write(space + "\n");
			fw.write(space + "private String state;\n");
			fw.write(space + "private List<" + nomeEntidade + "> items;\n");
			fw.write(space + "private " + nomeEntidade + " item;\n");
			fw.write(space + "\n");
			fw.write(space + "\n");
			fw.write(space + "public " + nomeEntidade + "Bean() {\n");
			fw.write(space + space
					+ "log.info(\"Bean constructor called.\");\n");
			fw.write(space + space + "limpaPesquisa();\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "\n");
			fw.write(space + "@PostConstruct\n");
			fw.write(space + "private void postConstruct() {\n");
			fw.write(space + space
					+ "log.info(\"Bean @PostConstruct called.\");\n");
			fw.write(space + space + "state = \"READ\";\n");
			fw.write(space + space + "items = service.list();\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void clearItems() {\n");
			fw.write(space + space + "if (items != null) {\n");
			fw.write(space + space + space + "items.clear();\n");
			fw.write(space + space + "}\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void clearItem() {\n");
			fw.write(space + space + "try {\n");
			fw.write(space
					+ space
					+ space
					+ "// Instantiating via reflection was used here for generic purposes\n");
			fw.write(space + space + space
					+ "item = " + nomeEntidade + ".class.newInstance();\n");
			fw.write(space
					+ space
					+ "} catch (InstantiationException | IllegalAccessException e) {\n");
			fw.write(space
					+ space
					+ space
					+ "	FacesUtils.addI18nError(\"generic.bean.unableToCleanViewData\");\n");
			fw.write(space + space + "}\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void create() {\n");
			fw.write(space + space + "service.create(item);\n");
			fw.write(space + space + "limpaPesquisa();\n");
			fw.write(space + space + "items = service.list();\n");
			fw.write(space + space + "item = null;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void update() {\n");
			fw.write(space + space + "service.update(item);\n");
			fw.write(space + space + "limpaPesquisa();\n");
			fw.write(space + space + "items = service.list();\n");
			fw.write(space + space + "item = null;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void delete() {\n");
			fw.write(space + space + "service.delete(item.getId());\n");
			fw.write(space + space + "limpaPesquisa();\n");
			fw.write(space + space + "items = service.list();\n");
			fw.write(space + space + "item = null;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void pesquisa(){\n");
			fw.write(space + "\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void limpaPesquisa(){\n");
			fw.write(space + "\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "@PreDestroy\n");
			fw.write(space + "private void preDestroy() {\n");
			fw.write(space + space
					+ "log.info(\"Bean @PreDestroy called.\");\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public String getState() {\n");
			fw.write(space + space + "return state;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void setState(String state) {\n");
			fw.write(space + space + "this.state = state;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public List<" + nomeEntidade + "> getItems() {\n");
			fw.write(space + space + "return items;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void setItems(List<" + nomeEntidade + "> items) {\n");
			fw.write(space + space + "this.items = items;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public " + nomeEntidade + " getItem() {\n");
			fw.write(space + space + "return item;\n");
			fw.write(space + "}\n");
			fw.write(space + "\n");
			fw.write(space + "public void setItem(" + nomeEntidade + " item) {\n");
			fw.write(space + space + "this.item = item;\n");
			fw.write(space + "}\n");
			fw.write("}");

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void criaViewJsf(String nomeTabela) {
		// criando o front end
		String nomeXhtml = transformaNomeColuna(nomeTabela);
		String nomeEntidade = transformaNomeEntidade(nomeTabela);

		File fileXhtml = new File(viewPath + nomeXhtml + ".xhtml");

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileXhtml);

			fw.write(
					"<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n");
			fw.write("<html xmlns='http://www.w3.org/1999/xhtml'\n");
			fw.write("xmlns:ui='http://xmlns.jcp.org/jsf/facelets'\n");
			fw.write("xmlns:h='http://xmlns.jcp.org/jsf/html'\n");
			fw.write("xmlns:f='http://xmlns.jcp.org/jsf/core'\n");
			fw.write("xmlns:c='http://xmlns.jcp.org/jsp/jstl/core'\n");
			fw.write("xmlns:p='http://primefaces.org/ui'\n");
			fw.write("xmlns:pe='http://primefaces.org/ui/extensions'\n");
			fw.write("xmlns:pt='http://xmlns.jcp.org/jsf/passthrough'\n");
			fw.write("xmlns:b='http://bootsfaces.net/ui'");
			fw.write("xmlns:fn='http://java.sun.com/jsp/jstl/functions'>\n");
			fw.write("\n"); 

			fw.write("<ui:composition template='/private/template/layout.xhtml'>\n");
			fw.write("<ui:define name='content'>\n");

			fw.write("<p:growl id='growl' autoUpdate='true' globalOnly='false'showDetail='false' />\n");

			fw.write("<h:panelGroup id='wrapper' layout='block' styleClass='wrapper'>\n");
			fw.write("<h:form id='form' prependId='false'>\n");

			// ------- Inicio Bloco pesquisa -------------------------
			fw.write("<h:panelGroup id='viewPanelGroup' layout='block'\n");

			fw.write("rendered=\"#{" + nomeXhtml + "Bean.state eq 'READ'}\"\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");

			fw.write("\t<p:panel id='searchPanel' header=\"#{i18n['operations.search']}\">\n");
			// implementar a pesquisa usando o conceito de entidade filter
			Statement stmt = conn.createStatement();

			ResultSet rset = stmt.executeQuery("SELECT * from " + nomeTabela);

			ResultSetMetaData rsmd = rset.getMetaData();

			// retorna o numero total de colunas
			int numColumns = rsmd.getColumnCount();

			for (int i = 0; i < numColumns; i++) {

				fw.write("\n"); 

				// o label
				fw.write("\t\t<h:outputText value='" + rsmd.getColumnName(i + 1).toUpperCase() + ":' />\n");
				String nomeColuna = rsmd.getColumnName(i + 1);

				/**
				 * Se for uma fk
				 */
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { 
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1).toUpperCase());

					fw.write("\t\t<p:selectOneMenu id='" + nomeColuna + "'\n");
					fw.write("\t\t\tvalue='#{" + nomeXhtml + "Bean." + "item." + fk.getPkColumnName() + "}' label='" + nomeColuna.toUpperCase() + "'\n");
					fw.write("\t\t\t converter='#{itemConverter}'>\n");
					fw.write("\t\t\t<f:selectItem itemLabel='Escolha' itemValue='' />\n");		
					fw.write("\t\t\t<f:selectItems value='#{" + nomeXhtml + "Bean.lista" + fk.getFkColumnName() + "}'\n");	
					fw.write("\t\t\tvar='item' itemValue='#{item}' itemLabel='#{item.descricao}' />\n");
					fw.write("\t\t</p:selectOneMenu>\n");			
						

				} else { // campo comum
					
					fw.write("\t\t<p:inputText id='" + nomeColuna + "'\n");
					fw.write("\t\t\tvalue='#{" + nomeXhtml + "Bean.itemFilter." + nomeColuna + "}'>\n ");
					fw.write("\t\t</p:inputText>\n");
					
				}

			}

			fw.write("\t</p:panel>\n");

			fw.write("<br style='clear: left;' />\n");

			fw.write("\t<p:panel id='viewPanel' header=\"#{i18n['registros']}\">\n");
			// implementar a table result
			fw.write("\t<p:dataTable id='mainDataTable' value='#{" + nomeXhtml + "Bean.items}'");
			fw.write("\tvar='item'>");	
			for (int i = 0; i < numColumns; i++) {
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { 
					continue; // na table, implementar mais tarde o modo de mostrar as fks
				}
				fw.write("\t\t<p:column headerText='#' width='30' style='text-align: center;'>\n");
				fw.write("\t\t\t\t<h:outputText value='#{item." + rsmd.getColumnName(i + 1) + "}' />\n");
				fw.write("\t\t</p:column>\n");
				fw.write("\n");
			

			}
			fw.write("\t</p:dataTable>\n");
			
			
			fw.write("\t</p:panel>\n");
			fw.write("</div>\n");
			fw.write("</div>\n");
			fw.write("\t</h:panelGroup>\n");
			// -------- Fim Bloco pesquisa --------------------------------
			
			
			
			// ------- Inicio Bloco de edi��o do registro
			// -------------------------
			fw.write("<h:panelGroup id='editPanelGroup' layout='block'\n");
			fw.write("rendered=\"#{" + nomeXhtml + "Bean.state eq 'CREATE' or countryBean.state eq 'UPDATE'}\"\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");
			fw.write("<p:panel id='editPanel'>\n");
			for (int i = 0; i < numColumns; i++) {

				fw.write("\n"); 

				// o label
				fw.write("\t\t<h:outputText value='" + rsmd.getColumnName(i + 1).toUpperCase() + ":' />\n");
				String nomeColuna = rsmd.getColumnName(i + 1);

				/**
				 * Se for uma fk
				 */
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { 
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1).toUpperCase());

					fw.write("\t\t<p:selectOneMenu id='" + nomeColuna + "'\n");
					fw.write("\t\t\tvalue='#{" + nomeXhtml + "Bean." + "item." + fk.getPkColumnName() + "}' label='" + nomeColuna.toUpperCase() + "'\n");
					fw.write("\t\t\t converter='#{itemConverter}'>\n");
					fw.write("\t\t\t<f:selectItem itemLabel='Escolha' itemValue='' />\n");		
					fw.write("\t\t\t<f:selectItems value='#{" + nomeXhtml + "Bean.lista" + fk.getFkColumnName() + "}'\n");	
					fw.write("\t\t\tvar='item' itemValue='#{item}' itemLabel='#{item.descricao}' />\n");
					fw.write("\t\t</p:selectOneMenu>\n");			
						

				} else { // campo comum
					
					fw.write("\t\t<p:inputText id='" + nomeColuna + "'\n");
					fw.write("\t\t\tvalue='#{" + nomeXhtml + "Bean.itemFilter." + nomeColuna + "}'>\n ");
					fw.write("\t\t</p:inputText>\n");
					
				}

			}
			fw.write("</p:panel>\n");
			fw.write("</div>\n");
			fw.write("</div>\n");
			fw.write("</h:panelGroup>\n");

			// ------ Inicio de Bloco de remo��o do registro
			fw.write("<h:panelGroup id='removePanelGroup' layout='block'\n");
			fw.write("rendered=\"#{" + nomeXhtml + "Bean.state eq 'UPDATE'}\"\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");
			fw.write("<p:panel id='removePanel'>\n");
			fw.write(" header=\"#{i18n['operations.delete']} #{i18n['country']}\">\n "
					+ " <div class='ui-grid-form ui-grid ui-grid-responsive'> " + "	<div class='ui-grid-row'>\n "
					+ "	<div class='ui-grid-col-12'> " + "			<h3>" + "				<h:outputFormat"
					+ "					value=\"#{i18n['operations.delete.areYouSure']}\">\n"
					+ "					<f:param value='#{countryBean.item.name}' />\n"
					+ "					</h:outputFormat>" + "			</h3>" + "		</div>\n" + "	</div>\n" + "	</div>\n"
					+ "	<f:facet name='footer'>" + "	<p:commandButton value=\"#{i18n['button.cancel']}\"\n"
					+ "		icon='ui-icon-close' process='@this' update='@form'"
					+ "		immediate='true' styleClass='buttonCancel'" + "		style='float: left;'>\n"
					+ "		<f:setPropertyActionListener target=\"#{countryBean.state}\"" + "			value='READ' />\n"
					+ "	</p:commandButton>" + "	<p:commandButton id='buttonRemove'"
					+ "		value=\"#{i18n['button.remove']}\""
					+ "		action='#{countryBean.delete}' icon='ui-icon-trash'"
					+ "		process='@this' update='@form' style='float: right;'>\n"
					+ "		<f:setPropertyActionListener target=\"#{countryBean.state}\"" + "			value='READ' />\n"
					+ "	</p:commandButton>" + "	<div style='clear: both;'>\n</div>\n" + "	</f:facet>\n" + "	</p:panel>\n"
					+ "	</div>\n" + "	</div>\n");

			// --- Fechando o xhtml -------
			fw.write("</h:panelGroup>\n");
			fw.write("</h:form>\n");
			fw.write("</h:panelGroup>\n");
			fw.write("</ui:define>\n");
			fw.write("</ui:composition>\n");
			fw.write("</html>\n");
			
			fw.flush();
			fw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
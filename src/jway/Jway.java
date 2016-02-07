package jway;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.postgresql.*;

public class Jway {
	private Map<String, CampoFk> mapCamposFk = new HashMap<String, CampoFk>();
	private Map<String, String> mapEntidades = new HashMap<String, String>();
	private Connection conn;
	private String space = "   ";
	private String nomePacote;
	private String modelPath;
	private String servicePath;
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
		Jway jway = new Jway();
		jway.processa();

	}

	public Jway() {
		// registrar o Driver JDBC do banco de dados, neste caso estou usando o
		// da Oracle
		try {
			if (isPostgresql) {

				DriverManager.registerDriver(new org.postgresql.Driver());
				conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + nomeBanco, user, password);

				// recuperar a classe DatabaseMetadaData a partir da conexao
				// criada
				dbmd = conn.getMetaData();
			} else {
				DriverManager.registerDriver(new com.mysql.jdbc.Driver());
				conn = DriverManager.getConnection("jdbc:mysql://107.161.176.58:3306/fitapp", "fitapp", "abc123#");
				// recuperar a classe DatabaseMetadaData a partir da conexao
				// criada
				dbmd = conn.getMetaData();

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void processa() {
		try {
			nomePacote = "br.com.jway"; // sto vai ser informado na tela

			montaNomePastas();
			criaPastas();

			String[] tableTypes = { "TABLE" };

			DatabaseMetaData metaData;
			metaData = conn.getMetaData();
			ResultSet listaTabelas = metaData.getTables(null, null, "%", tableTypes);
			// String nomeTabela = "unidade_orcamentaria";

			System.out.println("Versao do Driver JDBC = " + dbmd.getDriverVersion());
			System.out.println("Versao do Banco de Dados = " + dbmd.getDatabaseProductVersion());
			System.out.println("Suporta Select for Update? = " + dbmd.supportsSelectForUpdate());
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
			e.printStackTrace();
		}
	}

	private void criaAmbiente() {

	}

	private void montaNomePastas() {
		String nomeDiretorio = nomePacote.replace(".", "/");
		modelPath = "/TEMP/src/" + nomeDiretorio + "/model/";
		daoPath = "/TEMP/src/" + nomeDiretorio + "/dao/";
		daoImplPath = "/TEMP/src/" + nomeDiretorio + "/dao/impl";
		servicePath = "/TEMP/src/" + nomeDiretorio + "/service/";
		beanPath = "/TEMP/src/" + nomeDiretorio + "/view/";
		viewPath = "/TEMP/WebContent/" + nomeDiretorio + "/view/";

	}

	private void armazenaFks(String nomeTabela) {
		// Buscando as foreign keys da tabela aldeia
		DatabaseMetaData metaData;
		try {
			metaData = conn.getMetaData();
			ResultSet foreignKeys = metaData.getImportedKeys(conn.getCatalog(), null, nomeTabela);

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
			e.printStackTrace();
		}

	}

	private void criaPastas() {
		File diretorio;
		try {
			diretorio = new File(modelPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - " + diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Erro ao criar o diretorio model");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(servicePath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - " + diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Erro ao criar o diretorio service");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(daoPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - " + diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Erro ao criar o diretorio dao");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(daoImplPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - " + diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Erro ao criar o diretorio dao Impl");
			System.out.println(ex);
			ex.printStackTrace();
		}

		try {
			diretorio = new File(beanPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - " + diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Erro ao criar o diretorio bean");
			System.out.println(ex);
			ex.printStackTrace();
		}

	}

	private void criaEntidade(String nomeTabela) throws SQLException {

		Statement stmt = conn.createStatement();
		// Tabela a ser analisada

		ResultSet rset = stmt.executeQuery("SELECT * from " + nomeTabela);

		ResultSetMetaData rsmd = rset.getMetaData();

		// retorna o numero total de colunas
		int numColumns = rsmd.getColumnCount();
		System.out.println("tabela " + nomeTabela + ": Total de Colunas = " + numColumns);

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

			fw.write("\n");

			fw.write("@Entity \n");
			fw.write("@Table(name=\"" + nomeTabela + "\")\n");

			fw.write("public class " + nomeEntidade + " implements Serializable {\n");

			fw.write("\n");

			fw.write(space + "private static final long serialVersionUID = 1L;\n");

			// definindo as colunas
			for (int i = 0; i < numColumns; i++) {

				fw.write("\n");

				if (rsmd.getColumnName(i + 1).equals("id")) {
					fw.write(space + "@Id\n");
					fw.write(space + "@GeneratedValue(strategy = GenerationType.IDENTITY)\n");

				}

				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { // se
																						// for
																						// uma
																						// fk
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1).toUpperCase());

					fw.write(space + "@ManyToOne");
					fw.write("\n");
					fw.write(space + "@JoinColumn(name = \"" + fk.getFkColumnName() + "\")");
					fw.write("\n");

					fw.write(space + "private " + transformaNomeEntidade(fk.getPkTableName()) + " "
							+ transformaNomeColuna(fk.getPkTableName().toLowerCase()) + ";\n");

				} else {
					fw.write(space + "@Column(name=\"" + rsmd.getColumnName(i + 1) + "\")\n");
					fw.write(space + "private "
							+ transformaTipo(rsmd.getColumnTypeName(i + 1), rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase().contains("id"))
							+ " " + transformaNomeColuna(rsmd.getColumnName(i + 1)) + ";\n");
				}

			}

			// gets e sets
			for (int i = 0; i < numColumns; i++) {
				fw.write("\n");
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { // se
																						// for
																						// uma
																						// fk
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1).toUpperCase());

					fw.write("\n");

					fw.write(space + "public  " + transformaNomeEntidade(fk.getPkTableName()) + " " + " " + "get"
							+ transformaNomeEntidade(fk.getPkTableName()) + "() { \n");
					fw.write(space + space + "return " + transformaNomeColuna(fk.getPkTableName()) + ";\n");
					fw.write(space + "}\n");

					fw.write(space + "public void " + " " + "set" + transformaNomeEntidade(fk.getPkTableName())

							+ "(" + transformaNomeEntidade(fk.getPkTableName()) + " "
							+ transformaNomeColuna(fk.getPkTableName()) + ") { \n");
					fw.write(space + space + "this." + transformaNomeColuna(fk.getPkTableName()) + " = "
							+ transformaNomeColuna(fk.getPkTableName()) + ";\n");
					fw.write(space + "}\n");

				} else {
					fw.write(space + "public "
							+ transformaTipo(rsmd.getColumnTypeName(i + 1), rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase().contains("id"))
							+ " " + "get" + transformaNomeColunaPrimeiroCaracterMaiusculo(rsmd.getColumnName(i + 1))
							+ "() { \n");
					fw.write(space + space + "return " + transformaNomeColuna(rsmd.getColumnName(i + 1)) + ";\n");
					fw.write(space + "}\n");

					fw.write(space + "public void " + " " + "set"
							+ transformaNomeColunaPrimeiroCaracterMaiusculo(rsmd.getColumnName(i + 1)) + "("
							+ transformaTipo(rsmd.getColumnTypeName(i + 1), rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase().contains("id"))
							+ " " + transformaNomeColuna(rsmd.getColumnName(i + 1)) + ") { \n");
					fw.write(space + space + "this." + transformaNomeColuna(rsmd.getColumnName(i + 1)) + " = "
							+ transformaNomeColuna(rsmd.getColumnName(i + 1)) + ";\n");
					fw.write(space + "}\n");
				}

			}

			fw.write("}"); // final da classe

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String transformaNomeEntidade(String nomeTabela) {

		String aux = nomeTabela.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = new String();

		for (int i = 0; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase() + pedacos[i].substring(1);
		}

		return aux;
	}

	private static String transformaNomeColunaPrimeiroCaracterMaiusculo(String columnName) {

		String aux = columnName.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = pedacos[0].substring(0, 1).toUpperCase() + pedacos[0].substring(1);

		for (int i = 1; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase() + pedacos[i].substring(1);
		}
		return aux;

	}

	private static String transformaNomeColuna(String columnName) {

		String aux = columnName.toLowerCase();
		String[] pedacos = aux.split("_");

		aux = pedacos[0];

		for (int i = 1; i < pedacos.length; i++) {
			aux = aux + pedacos[i].substring(0, 1).toUpperCase() + pedacos[i].substring(1);
		}
		return aux;

	}

	private static String transformaTipo(String tipo, int decimais, boolean campoId) {
		tipo = tipo.toLowerCase();

		// para o postgresql

		if (tipo.equals("numeric")) {
			if (decimais == 0)
				return ("Long");
			else
				return ("BigDecimal");
		}
		if (tipo.equals("varchar"))
			return "String";
		if (tipo.contains("date"))
			return "Date";
		if (tipo.contains("int")) {
			return "Long";
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
			return "Blob";
		}

		if (tipo.equals("text")) {
			return "String";
		}

		return tipo;

	}

	private void criaDao(String nomeTabela) {
		// criando a interface
		String nomeEntidade = transformaNomeEntidade(nomeTabela);
		String nomeInterface = nomeEntidade + "Dao";

		File fileInterface = new File(daoPath + nomeInterface + ".java");

		String space = "   ";
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

			fw.write(space + nomeEntidade + " read(Long id);\n");

			fw.write(space + "void create(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ");\n");

			fw.write(space + nomeEntidade + " update(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade)
					+ ");\n");

			fw.write(space + "void delete(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ");\n");

			fw.write(space + "void delete(Long id);\n");

			fw.write(space + "List<" + nomeEntidade + "> list(Long id);\n");

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

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileDaoImpl);

			fw.write("package " + nomePacote + ".dao.impl; \n");

			fw.write("\n");

			fw.write("java.util.List;\n");
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
			fw.write("public class " + nomeInterface + "Impl implements " + nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space + "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");
			fw.write(space + "@PersistenceContext \n");
			fw.write(space + "protected EntityManager em;\n");
			fw.write("\n");
			fw.write(space + "UaiCriteria<" + nomeEntidade + "> uaiCriteria;\n");
			fw.write(space + "@Override\n");
			fw.write(space + "StringBuilder jpql = new StringBuilder()\n"); //
			fw.write(space + ".append('SELECT x \n");
			// parei aqui. HB

			fw.write(space + "}\n");

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

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileInterface);

			fw.write("package " + nomePacote + ".service; \n");

			fw.write("\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");

			fw.write("\n");

			fw.write("public interface " + nomeInterface + " extends Serializable {\n");

			fw.write("\n");

			fw.write(space + "public boolean existe(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade)
					+ ");\n\n");
			fw.write(space + "public void adiciona(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade)
					+ ");\n\n");
			fw.write(
					space + "public void exclui(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(
					space + "public void altera(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public List<" + nomeEntidade + "> lista();\n\n");

			fw.write(space + "public Object busca" + nomeEntidade + "PeloId(long id);\n\n");

			fw.write("}"); // final da interface

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		criaServiceImpl(nomeEntidade, nomeInterface);

	}

	private void criaServiceImpl(String nomeEntidade, String nomeInterface) {
		File fileServiceImpl = new File(servicePath + nomeInterface + "Impl.java");
		String nomeInterfaceDao = nomeEntidade + "Dao";

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileServiceImpl);

			fw.write("package " + nomePacote + ".service; \n");

			fw.write("\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".dao." + nomeInterfaceDao + ";\n");
			fw.write("import " + nomePacote + ".service." + nomeInterface + ";\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;");
			fw.write("import org.springframework.stereotype.Service;");
			fw.write("\n");
			fw.write("import org.hibernate.SessionFactory;\n");

			fw.write("@Service(\"" + nomeInterface + "\") \n");
			fw.write("public class " + nomeInterface + "Impl implements " + nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space + "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");
			fw.write(space + "@Autowired \n");
			fw.write(space + "private SessionFactory sessionFactory;\n");
			fw.write("\n");
			fw.write(space + "@Autowired \n");
			fw.write(space + "private " + nomeInterfaceDao + " dao;\n");
			fw.write("\n");
			fw.write(space + "StringBuilder hql; \n");
			fw.write("\n");

			// --
			fw.write(space + "public boolean existe(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade)
					+ "){\n");

			fw.write(space + space + "return dao.existe(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(
					space + "public void adiciona(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.adiciona(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void exclui(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.exclui(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void altera(" + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.altera(" + transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public List<" + nomeEntidade + "> lista(){\n");
			fw.write(space + space + "return dao.lista();\n ");

			fw.write(space + "}\n");

			// --

			fw.write(space + "public Object busca" + nomeEntidade + "PeloId(long id){\n");
			fw.write(space + space + "return dao.busca" + nomeEntidade + "PeloId(id);\n");

			fw.write(space + "}\n");

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

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileBean);

			fw.write("package " + nomePacote + ".view; \n");

			fw.write("\n");
			fw.write("import java.io.Serializable;\n" + "import java.util.Arrays;\n" + "import java.util.Date;\n"
					+ "import java.util.List;\n");

			fw.write("import javax.annotation.PostConstruct;\n" + "import javax.faces.bean.ManagedBean;\n"
					+ "import javax.faces.bean.ViewScoped;\n" + "import org.primefaces.event.ScheduleEntryMoveEvent;\n"
					+ "import org.primefaces.event.ScheduleEntryResizeEvent;\n"
					+ "import org.primefaces.event.SelectEvent;\n"
					+ "import org.primefaces.model.DefaultScheduleEvent;\n"
					+ "import org.primefaces.model.DefaultScheduleModel;\n"
					+ "import org.primefaces.model.ScheduleEvent;\n" + "import org.primefaces.model.ScheduleModel;\n"
					+ "import org.springframework.beans.factory.annotation.Autowired;\n"
					+ "import org.springframework.stereotype.Component;\n" + "import util.Util;\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;\n");
			fw.write("import org.springframework.stereotype.Repository;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("\n");

			fw.write("@ViewScoped\n" + "@Component \n" + "@ManagedBean(name = '" + nomeEntidade + "Bean'" + ")\n");
			fw.write("publics  class " + nomeEntidade + "Bean" + "Impl implements Serializable " + "{\n");
			fw.write("\n");

			fw.write(space + "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");

			fw.write(space + "private " + nomeEntidade + " " + transformaNomeColuna(nomeEntidade) + ";\n");
			fw.write("\n");
			fw.write(space + "@Autowired\n");
			fw.write(
					space + "private " + nomeEntidade + "Service " + transformaNomeColuna(nomeEntidade) + "Service;\n");
			fw.write("\n");
			fw.write(space + "private final String MSG_ERRO_NAO_PREENCHIMENTO_CAMPOS = 'Campo deve ser informado';\n");
			fw.write("\n");

			fw.write(space + "private List<" + nomeEntidade + "> lista;\n");
			fw.write("\n");

			fw.write(space + "public " + nomeEntidade + "() {\n\n");
			fw.write(space + "}\n");

			fw.write("\n");
			fw.write(space + "public " + transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade) + " get"
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade) + "() {\n");
			fw.write(space + space + "return " + transformaNomeColuna(nomeEntidade) + ";\n");
			fw.write(space + "}\n");
			fw.write("\n");

			fw.write(space + "public void set" + transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade) + "("
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade) + " "
					+ transformaNomeColuna(nomeEntidade) + ") {\n");
			fw.write(space + space + "this." + transformaNomeColuna(nomeEntidade) + " = "
					+ transformaNomeColuna(nomeEntidade) + ";\n");

			fw.write(space + "}\n");

			fw.write("\n");
			fw.write(space + "public List<" + transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade) + "> get"
					+ "lista" + "() {\n");
			fw.write(space + space + "return lista;\n");
			fw.write(space + "}\n");
			fw.write("\n");

			fw.write(space + "public void setLista(List<" + transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ "> " + "lista) {\n");
			fw.write(space + space + "this.lista = " + "lista;\n");

			fw.write(space + "}\n");

			fw.write("\n");

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
			fw.write("xmlns:pe='http://primefaces.org/ui/extensions'>\n");
			fw.write("xmlns:pt='http://xmlns.jcp.org/jsf/passthrough'\n");
			fw.write("xmlns:b='http://bootsfaces.net/ui'");
			fw.write("xmlns:fn='http://java.sun.com/jsp/jstl/functions'");

			fw.write("<ui:composition template='/private/template/layout.xhtml'>\n");
			fw.write("<ui:define name='content'>\n");

			fw.write("<p:growl id='growl' autoUpdate='true' globalOnly='false'showDetail='false' />\n");

			fw.write("<h:panelGroup id='wrapper' layout='block' styleClass='wrapper'>\n");
			fw.write("<h:form id='form' prependId='false'>\n");

			// ------- Inicio Bloco pesquisa -------------------------
			fw.write("<h:panelGroup id='viewPanelGroup' layout='block'\n");

			fw.write("rendered='#{" + nomeXhtml + "Bean.state eq 'READ'}'\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");

			fw.write("<p:panel id='searchPanel' header='#{i18n['operations.search']}'>\n");
			// implementar a pesquisa usando o conceito de entidade filter
			Statement stmt = conn.createStatement();
			// Tabela a ser analisada

			ResultSet rset = stmt.executeQuery("SELECT * from " + nomeTabela);

			ResultSetMetaData rsmd = rset.getMetaData();

			// retorna o numero total de colunas
			int numColumns = rsmd.getColumnCount();
			System.out.println("tabela " + nomeTabela + ": Total de Colunas = " + numColumns);

			// definindo as colunas
			for (int i = 0; i < numColumns; i++) {

				fw.write("\n");

				// o label
				fw.write("<h:outputText value='" + rsmd.getColumnName(i + 1).toUpperCase() + ":' />");
				
				
				if (mapCamposFk.containsKey(rsmd.getColumnName(i + 1).toUpperCase())) { // se
																						// for
																						// uma
																						// fk
					CampoFk fk = mapCamposFk.get(rsmd.getColumnName(i + 1).toUpperCase());

					fw.write(space + "@JoinColumn(name = \"" + fk.getFkColumnName() + "\")");
					fw.write("\n");

					fw.write(space + "private " + transformaNomeEntidade(fk.getPkTableName()) + " "
							+ transformaNomeColuna(fk.getPkTableName().toLowerCase()) + ";\n");

				} else {
					fw.write(space + "@Column(name=\"" + rsmd.getColumnName(i + 1) + "\")\n");
					fw.write(space + "private "
							+ transformaTipo(rsmd.getColumnTypeName(i + 1), rsmd.getScale(i + 1),
									rsmd.getColumnName(i + 1).toLowerCase().contains("id"))
							+ " " + transformaNomeColuna(rsmd.getColumnName(i + 1)) + ";\n");
				}

			}

			fw.write("</p:panel>\n");

			fw.write("<br style='clear: left;' />\n");

			fw.write("<p:panel id='viewPanel' header='#{i18n['registros']}'>\n");
			// implementar a table result
			fw.write("</p:panel>\n");
			fw.write("</div>\n");
			fw.write("</div>\n");
			fw.write("</h:panelGroup>\n");
			// -------- Fim Bloco pesquisa --------------------------------

			// ------- Inicio Bloco de edição do registro
			// -------------------------
			fw.write("<h:panelGroup id='editPanelGroup' layout='block'\n");
			fw.write("rendered='#{" + nomeXhtml + "Bean.state eq 'CREATE' or countryBean.state eq 'UPDATE'}'\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");
			fw.write("<p:panel id='editPanel'>\n");
			// implementar o edit
			fw.write("</p:panel>\n");
			fw.write("</div>\n");
			fw.write("</div>\n");
			fw.write("</h:panelGroup>\n");

			// ------ Inicio de Bloco de remoção do registro
			fw.write("<h:panelGroup id='removePanelGroup' layout='block'\n");
			fw.write("rendered='#{" + nomeXhtml + "Bean.state eq 'UPDATE'}'\n");
			fw.write("styleClass='ui-grid ui-grid-responsive'>\n");
			fw.write("<div class='ui-grid-row'>\n");
			fw.write("<div class='ui-grid-col-12'>\n");
			fw.write("<p:panel id='removePanel'>\n");
			fw.write(" header='#{i18n['operations.delete']} #{i18n['country']}'> "
					+ " <div class='ui-grid-form ui-grid ui-grid-responsive'> " + "	<div class='ui-grid-row'> "
					+ "	<div class='ui-grid-col-12'> " + "			<h3>" + "				<h:outputFormat"
					+ "					value='#{i18n['operations.delete.areYouSure']}'>"
					+ "					<f:param value='#{countryBean.item.name}' />"
					+ "					</h:outputFormat>" + "			</h3>" + "		</div>" + "	</div>" + "	</div>"
					+ "	<f:facet name='footer'>" + "	<p:commandButton value='#{i18n['button.cancel']}'"
					+ "		icon='ui-icon-close' process='@this' update='@form'"
					+ "		immediate='true' styleClass='buttonCancel'" + "		style='float: left;'>"
					+ "		<f:setPropertyActionListener target='#{countryBean.state}'" + "			value='READ' />"
					+ "	</p:commandButton>" + "	<p:commandButton id='buttonRemove'"
					+ "		value='#{i18n['button.remove']}'"
					+ "		action='#{countryBean.delete}' icon='ui-icon-trash'"
					+ "		process='@this' update='@form' style='float: right;'>"
					+ "		<f:setPropertyActionListener target='#{countryBean.state}'" + "			value='READ' />"
					+ "	</p:commandButton>" + "	<div style='clear: both;'></div>" + "	</f:facet>" + "	</p:panel>"
					+ "	</div>" + "	</div>\n");

			// --- Fechando o xhtml -------
			fw.write("</h:form>\n");
			fw.write("</h:panelGroup>\n");
			fw.write("</ui:define>\n");
			fw.write("</ui:composition>\n");
			fw.write("</html>\n");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

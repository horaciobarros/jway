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
	private String beanPath;
	private String viewPath;
	private DatabaseMetaData dbmd;
	private boolean isPostgresql = false;

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
				conn = DriverManager
						.getConnection("jdbc:postgres://localhost:5432/xsmile",
								"root", "root");

				// recuperar a classe DatabaseMetadaData a partir da conexao
				// criada
				dbmd = conn.getMetaData();
			} else {
				DriverManager.registerDriver(new com.mysql.jdbc.Driver());
				conn = DriverManager
						.getConnection("jdbc:mysql://localhost:3306/xsmile",
								"root", "root");

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
			nomePacote = "xsmile"; // isto vai ser informado na tela
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
		modelPath = "/TEMP/src/" + nomePacote + "/model/";
		daoPath = "/TEMP/src/" + nomePacote + "/dao/";
		servicePath = "/TEMP/src/" + nomePacote + "/service/";
		beanPath = "/TEMP/src/" + nomePacote + "/view/";
		viewPath = "/TEMP/WebContent/" + nomePacote + "/view/";

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

	private void criaPastas() {
		File diretorio;
		try {
			diretorio = new File(modelPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - "
					+ diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"Erro ao criar o diretorio model");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(servicePath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - "
					+ diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"Erro ao criar o diretorio service");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(daoPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - "
					+ diretorio.exists());
		} catch (Exception ex) {
			JOptionPane
					.showMessageDialog(null, "Erro ao criar o diretorio dao");
			System.out.println(ex);
			ex.printStackTrace();
		}
		try {
			diretorio = new File(beanPath);
			diretorio.mkdirs();
			System.out.println(diretorio.getAbsolutePath() + " - "
					+ diretorio.exists());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"Erro ao criar o diretorio bean");
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

		return tipo;

	}

	private void criaViewJsf(String nomeTabela) {
		// TODO Auto-generated method stub

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
			fw.write("import org.hibernate.SessionFactory;\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;\n");
			fw.write("import org.springframework.stereotype.Repository;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");

			fw.write("\n");

			fw.write("public interface " + nomeInterface
					+ " extends Serializable {\n");

			fw.write("\n");

			fw.write(space + "public boolean existe(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void adiciona(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void exclui(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void altera(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public List<" + nomeEntidade + "> lista();\n\n");

			fw.write(space + "public Object busca" + nomeEntidade
					+ "PeloId(long id);\n\n");

			fw.write("}"); // final da interface

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		criaDaoImpl(nomeEntidade, nomeInterface);

	}

	private void criaDaoImpl(String nomeEntidade, String nomeInterface) {
		File fileDaoImpl = new File(daoPath + nomeInterface + "Impl.java");

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileDaoImpl);

			fw.write("package " + nomePacote + ".dao; \n");

			fw.write("\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import org.hibernate.SessionFactory;\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;\n");
			fw.write("import org.springframework.stereotype.Repository;\n");
			fw.write("import org.hibernate.Query;\n");
			fw.write("import org.hibernate.Transaction;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".dao.*;\n");

			fw.write("\n");
			fw.write("@Repository \n");
			fw.write("public class " + nomeInterface + "Impl implements "
					+ nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");
			fw.write(space + "@Autowired \n");
			fw.write(space + "private SessionFactory sessionFactory;\n");
			fw.write("\n");
			fw.write(space + "@Autowired \n");
			fw.write(space + "private Dao<" + nomeEntidade + "> dao;\n");
			fw.write("\n");
			fw.write(space + "StringBuilder hql; \n");
			fw.write("\n");

			// --
			fw.write(space + "public boolean existe(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space
					+ space
					+ "Transaction tx = sessionFactory.getCurrentSession().beginTransaction();\n");
			fw.write(space
					+ space
					+ "Query query = sessionFactory.getCurrentSession().createQuery("
					+ "\"from " + nomeEntidade + " e where e.id = :pId \");\n");

			fw.write(space + space + "query.setParameter(\"pId\", "
					+ transformaNomeColuna(nomeEntidade) + ".getId());\n");

			fw.write(space + space + "List lista = query.list();\n"
					+ "tx.commit()\n;"
					+ "boolean encontrado = !lista.isEmpty();\n"
					+ "return encontrado;\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void adiciona(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.save("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void exclui(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.delete("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void altera(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.update("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public List<" + nomeEntidade + "> lista(){\n");
			fw.write(space + space + "return (List<" + nomeEntidade
					+ ">) this.dao.find(\"FROM " + nomeEntidade + " e \");\n ");

			fw.write(space + "}\n");

			// --

			fw.write(space + "public Object busca" + nomeEntidade
					+ "PeloId(long id){\n");
			fw.write(space
					+ space
					+ "Transaction tx = sessionFactory.getCurrentSession().beginTransaction();\n");
			fw.write(space
					+ space
					+ "Query query = sessionFactory.getCurrentSession().createQuery("
					+ "\"from " + nomeEntidade + " e where e.id = :pId \");\n");

			fw.write(space + space + "query.setParameter(\"pId\", " + "id);\n");

			fw.write(space + space + "List lista = query.list();\n"
					+ " return (" + nomeEntidade + ") lista.get(0);\n");

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

			fw.write("public interface " + nomeInterface
					+ " extends Serializable {\n");

			fw.write("\n");

			fw.write(space + "public boolean existe(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void adiciona(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void exclui(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public void altera(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ");\n\n");
			fw.write(space + "public List<" + nomeEntidade + "> lista();\n\n");

			fw.write(space + "public Object busca" + nomeEntidade
					+ "PeloId(long id);\n\n");

			fw.write("}"); // final da interface

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		criaServiceImpl(nomeEntidade, nomeInterface);

	}

	private void criaServiceImpl(String nomeEntidade, String nomeInterface) {
		File fileServiceImpl = new File(servicePath + nomeInterface
				+ "Impl.java");
		String nomeInterfaceDao = nomeEntidade + "Dao";

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileServiceImpl);

			fw.write("package " + nomePacote + ".service; \n");

			fw.write("\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("import " + nomePacote + ".dao." + nomeInterfaceDao
					+ ";\n");
			fw.write("import " + nomePacote + ".service." + nomeInterface
					+ ";\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;");
			fw.write("import org.springframework.stereotype.Service;");
			fw.write("\n");
			fw.write("import org.hibernate.SessionFactory;\n");

			fw.write("@Service(\"" + nomeInterface + "\") \n");
			fw.write("public class " + nomeInterface + "Impl implements "
					+ nomeInterface + "{\n");

			fw.write("\n");
			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
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
			fw.write(space + "public boolean existe(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");

			fw.write(space + space + "return dao.existe("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void adiciona(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.adiciona("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void exclui(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.exclui("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public void altera(" + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + "){\n");
			fw.write(space + space + "dao.altera("
					+ transformaNomeColuna(nomeEntidade) + ");\n");
			fw.write(space + "}\n");

			// --
			fw.write(space + "public List<" + nomeEntidade + "> lista(){\n");
			fw.write(space + space + "return dao.lista();\n ");

			fw.write(space + "}\n");

			// --

			fw.write(space + "public Object busca" + nomeEntidade
					+ "PeloId(long id){\n");
			fw.write(space + space + "return dao.busca" + nomeEntidade
					+ "PeloId(id);\n");

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
		String nomeService = nomeEntidade + "Service";

		String space = "   ";
		try {
			FileWriter fw = new FileWriter(fileBean);

			fw.write("package " + nomePacote + ".view; \n");

			fw.write("\n");
			fw.write("import java.io.Serializable;\n"
					+ "import java.util.Arrays;\n" + "import java.util.Date;\n"
					+ "import java.util.List;\n");

			fw.write("import javax.annotation.PostConstruct;\n"
					+ "import javax.faces.bean.ManagedBean;\n"
					+ "import javax.faces.bean.ViewScoped;\n"
					+ "import org.primefaces.event.ScheduleEntryMoveEvent;\n"
					+ "import org.primefaces.event.ScheduleEntryResizeEvent;\n"
					+ "import org.primefaces.event.SelectEvent;\n"
					+ "import org.primefaces.model.DefaultScheduleEvent;\n"
					+ "import org.primefaces.model.DefaultScheduleModel;\n"
					+ "import org.primefaces.model.ScheduleEvent;\n"
					+ "import org.primefaces.model.ScheduleModel;\n"
					+ "import org.springframework.beans.factory.annotation.Autowired;\n"
					+ "import org.springframework.stereotype.Component;\n"
					+ "import util.Util;\n");

			fw.write("import java.io.*;\n");
			fw.write("import java.util.*;\n");
			fw.write("import org.springframework.beans.factory.annotation.Autowired;\n");
			fw.write("import org.springframework.stereotype.Repository;\n");
			fw.write("import " + nomePacote + ".model." + nomeEntidade + ";\n");
			fw.write("\n");

			fw.write("@ViewScoped\n" + "@Component \n"
					+ "@ManagedBean(name = '" + nomeEntidade + "Bean'" + ")\n");
			fw.write("publics  class " + nomeEntidade + "Bean"
					+ "Impl implements Serializable " + "{\n");
			fw.write("\n");

			fw.write(space
					+ "private static final long serialVersionUID = 1L;\n");
			fw.write("\n");

			fw.write(space + "private " + nomeEntidade + " "
					+ transformaNomeColuna(nomeEntidade) + ";\n");
			fw.write("\n");
			fw.write(space + "@Autowired\n");
			fw.write(space + "private " + nomeEntidade + "Service "
					+ transformaNomeColuna(nomeEntidade) + "Service;\n");
			fw.write("\n");
			fw.write(space
					+ "private final String MSG_ERRO_NAO_PREENCHIMENTO_CAMPOS = 'Campo deve ser informado';\n");
			fw.write("\n");

			fw.write(space + "private List<" + nomeEntidade + "> lista;\n");
			fw.write("\n");

			fw.write(space + "public " + nomeEntidade + "() {\n\n");
			fw.write(space + "}\n");

			fw.write("\n");
			fw.write(space
					+ "public "
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ " get"
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ "() {\n");
			fw.write(space + space + "return "
					+ transformaNomeColuna(nomeEntidade) + ";\n");
			fw.write(space + "}\n");
			fw.write("\n");

			fw.write(space
					+ "public void set"
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ "("
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ " " + transformaNomeColuna(nomeEntidade) + ") {\n");
			fw.write(space + space + "this."
					+ transformaNomeColuna(nomeEntidade) + " = "
					+ transformaNomeColuna(nomeEntidade) + ";\n");

			fw.write(space + "}\n");

			fw.write("\n");
			fw.write(space
					+ "public List<"
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
					+ "> get" + "lista" + "() {\n");
			fw.write(space + space + "return lista;\n");
			fw.write(space + "}\n");
			fw.write("\n");

			fw.write(space
					+ "public void setLista(List<"
					+ transformaNomeColunaPrimeiroCaracterMaiusculo(nomeEntidade)
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

}
